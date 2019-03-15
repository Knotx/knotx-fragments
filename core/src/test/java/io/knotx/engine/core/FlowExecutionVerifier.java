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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.core;

import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.core.impl.KnotEngineFactory;
import io.knotx.fragment.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;

class FlowExecutionVerifier {

  private FlowExecutionVerifier() {
    // hidden constructor
  }

  static void verifyExecution(VertxTestContext testContext, Vertx vertx, KnotFlow knotFlow,
      Consumer<List<FragmentEvent>> successConsumer) throws Throwable {
    verifyExecution(testContext, vertx, Collections.singletonList(knotFlow), successConsumer);
  }

  static void verifyExecution(VertxTestContext testContext, Vertx vertx, List<KnotFlow> knotFlow,
      Consumer<List<FragmentEvent>> successConsumer) throws Throwable {
    // prepare
    List<FragmentEvent> events = knotFlow.stream()
        .map(flow -> new FragmentEvent(new Fragment("type", new JsonObject(), "body"), flow))
        .collect(
            Collectors.toList());
    KnotEngine engine = KnotEngineFactory.get(vertx);

    // execute
    Single<List<FragmentEvent>> execute = engine.execute(events, new ClientRequest());

    // verifyLogEntries
    execute.subscribe(
        onSuccess -> testContext.verify(() -> {
          successConsumer.accept(onSuccess);
          testContext.completeNow();
        }), testContext::failNow);

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  static void verifyFailingSingle(VertxTestContext testContext, Vertx vertx, KnotFlow knotFlow)
      throws Throwable {
    // given
    KnotEngine engine = KnotEngineFactory.get(vertx);

    // when
    Single<List<FragmentEvent>> execute = engine
        .execute(Collections
                .singletonList(
                    new FragmentEvent(new Fragment("type", new JsonObject(), "body"), knotFlow)),
            new ClientRequest());

    // then
    execute.subscribe(
        onSuccess -> testContext.failNow(new IllegalStateException()),
        error -> testContext.completeNow());

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
