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
package io.knotx.fragments.handler.action;

import static io.knotx.fragments.handler.action.CircuitBreakerActionFactory.FALLBACK_TRANSITION;
import static io.knotx.fragments.handler.api.actionlog.ActionLogMode.INFO;
import static io.knotx.fragments.handler.api.actionlog.ActionLogger.getStringLogEntry;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.action.CircuitBreakerActionFactory.CircuitBreakerAction;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.TimeoutException;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava.core.Future;

@ExtendWith(VertxExtension.class)
class CircuitBreakerActionTest {

  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(), "expectedBody");
  private static final int TIMEOUT_IN_MS = 500;

  @Test
  @DisplayName("Expect operation ends when doAction ends on time. Action logs from doAction present.")
  void expectOperationEnds(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerActionTest::apply, ActionLogger.create("tested", INFO));

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                List doActionsLog =  result.getActionLog().getJsonArray("doAction").getList();
                Assertions.assertTrue(doActionsLog.size() == 1);
                JsonObject actionLogs = (JsonObject) doActionsLog.get(0);
                Assertions.assertEquals("success", actionLogs.getJsonObject("logs").getString("info"));
                Assertions.assertEquals(SUCCESS_TRANSITION, result.getTransition());
              });
          testContext.completeNow();
        }));

    //then
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect fallback transition when doAction times out and circuit breaker is in fallback mode.")
  void expectFallback(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setTimeout(TIMEOUT_IN_MS).setFallbackOnFailure(true);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        (fragmentContext, resultHandler) ->
            vertx.setTimer(1000,
                l ->
                    Future.succeededFuture(
                        new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION)
                    ).setHandler(resultHandler)),
        ActionLogger.create("tested", INFO));

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                Assertions.assertNotNull(getStringLogEntry("fallback", result.getActionLog()));
                Assertions.assertEquals(FALLBACK_TRANSITION, result.getTransition());
              });
          testContext.completeNow();
        }));

    //then
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect failure handler response with timeout exception when doAction times out.")
  void expectTimeout(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        (fragmentContext, resultHandler) ->
            vertx.setTimer(1000,
                l ->
                    Future.succeededFuture(
                        new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION)
                    ).setHandler(resultHandler)),
        ActionLogger.create("tested", INFO));

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.failing(result -> {
          testContext
              .verify(() -> Assertions.assertTrue(result instanceof TimeoutException));
          testContext.completeNow();
        }));

    //then
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private static void apply(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "success");
    Future.succeededFuture(new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION,
        actionLogger.toLog()))
        .setHandler(resultHandler);
  }

}