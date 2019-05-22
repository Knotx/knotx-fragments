/*
 * Copyright (C) 2019 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.knotx.fragments.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class FragmentsEngineConcurrencyTest {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FragmentsEngineConcurrencyTest.class);

  private static final int NUMBER_OF_PROCESSED_EVENTS = 10;
  private static final int BLOCKING_TIME_IN_MILLIS = 500;
  private static final int WAITING_TIME_IN_MILLIS =
      NUMBER_OF_PROCESSED_EVENTS * BLOCKING_TIME_IN_MILLIS / 2;

  private static final Function<FragmentContext, Single<FragmentResult>> BLOCKING_OPERATION = fragmentContext -> {
    try {
      System.out.println(Thread.currentThread().getName() + ": executing operation");
      Thread.sleep(BLOCKING_TIME_IN_MILLIS);
      System.out.println(Thread.currentThread().getName() + ": executing operation finished");
    } catch (InterruptedException e) {
      LOGGER.warn("Unexpected interrupted error!", e);
    }
    return Single.just(
        new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION));
  };

  @Test
  @DisplayName("Expect fragment events are evaluated in parallel.")
  void expectParallelEvaluationStrategy(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given

    List<FragmentEventContextTaskAware> events = Stream
        .generate(this::initFragmentEventContextTaskAware)
        .limit(NUMBER_OF_PROCESSED_EVENTS).collect(
            Collectors.toList());

    // when
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    CompletableFuture<Single<List<FragmentEvent>>> completableFuture = CompletableFuture
        .supplyAsync(() -> new FragmentsEngine(vertx).execute(events), executorService);

    // then
    verifyExecution(completableFuture, testContext);
  }

  private FragmentEventContextTaskAware initFragmentEventContextTaskAware() {
    ActionNode graphNode = new ActionNode("id", BLOCKING_OPERATION,
        Collections.emptyMap());
    Fragment fragment = new Fragment("snippet", new JsonObject(), "some body");

    return new FragmentEventContextWithTask(new Task("task", graphNode),
        new FragmentEventContext(new FragmentEvent(fragment), new ClientRequest()));
  }

  private void verifyExecution(CompletableFuture<Single<List<FragmentEvent>>> future,
      VertxTestContext testContext) throws Throwable {
    // execute
    future.thenApply(result -> {
      result.subscribe(
          onSuccess -> {
            System.out.println("Completed");
            testContext.completeNow();
          }
          , testContext::failNow);
      return result;
    });

    assertTrue(testContext.awaitCompletion(WAITING_TIME_IN_MILLIS, TimeUnit.MILLISECONDS),
        "Blocking operations are not evaluated in parallel!");
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
