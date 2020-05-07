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

import static io.knotx.fragments.engine.Nodes.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentOperation;
import io.knotx.fragments.engine.api.FragmentEvent;
import io.knotx.fragments.engine.api.FragmentEventContext;
import io.knotx.fragments.engine.api.FragmentEventContextTaskAware;
import io.knotx.fragments.task.api.Task;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class FragmentsEngineOrderTest {

  private static final FragmentOperation TIME_CONSUMING_OPERATION =
      (fragmentContext, resultHandler) -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        Future.succeededFuture(
            new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION))
            .setHandler(resultHandler);
      };
  private static final FragmentOperation SIMPLE_OPERATION =
      (fragmentContext, resultHandler) -> Future.succeededFuture(
          new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);

  @Test
  @DisplayName("Expect fragments in incoming order")
  void expectCorrectOrder(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    List<FragmentEventContextTaskAware> events = Arrays.asList(
        initFragmentEventContextTaskAware("first fragment", TIME_CONSUMING_OPERATION),
        initFragmentEventContextTaskAware("second fragment", SIMPLE_OPERATION)
    );

    // when
    Single<List<FragmentEvent>> result = new FragmentsEngine(vertx).execute(events);

    // then
    verifyExecution(result, fragmentEvents -> testContext.verify(() -> {
      assertEquals(2, fragmentEvents.size());
      assertEquals("first fragment", fragmentEvents.get(0).getFragment().getBody());
      assertEquals("second fragment", fragmentEvents.get(1).getFragment().getBody());
    }), testContext);
  }

  private FragmentEventContextTaskAware initFragmentEventContextTaskAware(String fragmentBody,
      FragmentOperation operation) {
    Node graphNode = single("id", operation);
    Fragment fragment = new Fragment("snippet", new JsonObject(), fragmentBody);

    return new FragmentEventContextTaskAware(new Task("task", graphNode),
        new FragmentEventContext(new FragmentEvent(fragment), new ClientRequest()));
  }

  void verifyExecution(Single<List<FragmentEvent>> result,
      Consumer<List<FragmentEvent>> successConsumer,
      VertxTestContext testContext) throws Throwable {
    result.subscribe(
        onSuccess -> testContext.verify(() -> {
          successConsumer.accept(onSuccess);
          testContext.completeNow();
        }), testContext::failNow);

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
