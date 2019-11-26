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
import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.INFO;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.action.CircuitBreakerActionFactory.CircuitBreakerAction;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.actionlog.ActionInvocationLog;
import io.knotx.fragments.handler.api.actionlog.ActionLog;
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

@ExtendWith(VertxExtension.class)
class CircuitBreakerActionTest {

  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(), "expectedBody");
  private static final int TIMEOUT_IN_MS = 500;

  @Test
  @DisplayName("Expect operation ends when doAction ends on time.")
  void expectOperationEnds(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applySuccess,
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
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
  @DisplayName("Expect action logs from doAction when doAction ends on time.")
  void expectActionLogs(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applySuccessWithActionLogs,
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                ActionLog actionLog = new ActionLog(result.getNodeLog());
                List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
                String invocationCount = actionLog.getLogs().getString("invocationCount");

                Assertions.assertEquals(1, doActionsLogs.size());
                ActionInvocationLog invocationLog = doActionsLogs.get(0);

                Assertions.assertTrue(invocationLog.isSuccess());
                Assertions.assertNotNull(invocationLog.getDuration());
                Assertions.assertEquals("1", invocationCount);
                Assertions.assertEquals("action", invocationLog.getDoActionLog().getAlias());
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
  @DisplayName("Expect timeout transition when doAction times out and circuit breaker is in fallback mode.")
  void expectFallback(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setTimeout(TIMEOUT_IN_MS).setFallbackOnFailure(true).setMaxRetries(2);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);
    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions.applyTimeout(vertx),
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                Assertions.assertEquals(FALLBACK_TRANSITION, result.getTransition());

                ActionLog actionLog = new ActionLog(result.getNodeLog());
                String fallback = actionLog.getLogs().getString("fallback");
                String invocationCount = actionLog.getLogs().getString("invocationCount");

                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("TimeoutException"));
                Assertions.assertEquals("3", invocationCount);
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
        CircuitBreakerDoActions.applyTimeout(vertx),
        "tested", INFO, ERROR_TRANSITION);

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

  @Test
  @DisplayName("Expect fallback transition when doAction ends up _error transition and circuit breaker is in fallback mode.")
  void expectFallbackWhenDoActionEndsErrorTransition(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setFallbackOnFailure(true);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applyErrorTransition, "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                Assertions.assertEquals(FALLBACK_TRANSITION, result.getTransition());

                ActionLog actionLog = new ActionLog(result.getNodeLog());
                String fallback = actionLog.getLogs().getString("fallback");
                List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();

                Assertions.assertEquals(1, doActionsLogs.size());
                ActionInvocationLog invocationLog = doActionsLogs.get(0);
                Assertions.assertFalse(invocationLog.isSuccess());
                Assertions.assertNotNull(invocationLog.getDoActionLog());
                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("DoActionExecuteException"));
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
  @DisplayName("Expect fallback transition when doAction failure and circuit breaker is in fallback mode.")
  void expectFallbackWhenDoActionFailure(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setFallbackOnFailure(true);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applyFailure, "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          testContext
              .verify(() -> {
                Assertions.assertEquals(FALLBACK_TRANSITION, result.result().getTransition());

                ActionLog actionLog = new ActionLog(result.result().getNodeLog());
                String fallback = actionLog.getLogs().getString("fallback");
                List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
                Assertions.assertEquals(1, doActionsLogs.size());
                ActionInvocationLog invocationLog = doActionsLogs.get(0);

                Assertions.assertFalse(invocationLog.isSuccess());
                Assertions.assertNull(invocationLog.getDoActionLog());
                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("DoActionExecuteException"));
              });
          testContext.completeNow();
        });

    //then
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect fallback transition when doAction throws exception and circuit breaker is in fallback mode.")
  void expectTBla(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setFallbackOnFailure(true);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applyException,
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                Assertions.assertEquals(FALLBACK_TRANSITION, result.getTransition());

                ActionLog actionLog = new ActionLog(result.getNodeLog());
                String fallback = actionLog.getLogs().getString("fallback");
                String invocationCount = actionLog.getLogs().getString("invocationCount");

                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("ReplyException"));
                Assertions.assertEquals("1", invocationCount);
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
  @DisplayName("Expect success when first call end up with failure and second with success.")
  void expectActionLogsWithFailureAndSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    oneAfterAnotherTestTemplate(testContext, vertx,
        CircuitBreakerDoActions::applyFailure,CircuitBreakerDoActions::applySuccessWithActionLogs,
        result -> {
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(2, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());

            ActionInvocationLog invocationLog2 = doActionsLogs.get(1);
            Assertions.assertTrue(invocationLog2.isSuccess());
            Assertions.assertNotNull(invocationLog2.getDoActionLog());

          });
          testContext.completeNow();
        });
  }

  @Test
  @DisplayName("Expect success when first call end up with _error transition and second with success.")
  void expectActionLogsWithErrorAndSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    oneAfterAnotherTestTemplate(testContext, vertx,
        CircuitBreakerDoActions::applyErrorTransition,CircuitBreakerDoActions::applySuccessWithActionLogs,
        result -> {
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(2, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNotNull(invocationLog1.getDoActionLog());

            ActionInvocationLog invocationLog2 = doActionsLogs.get(1);
            Assertions.assertTrue(invocationLog2.isSuccess());
            Assertions.assertNotNull(invocationLog2.getDoActionLog());

          });
          testContext.completeNow();
        });
  }

  @Test
  @DisplayName("Expect fallback when first call ends up with failure and second with _error transition.")
  void expectFallbackActionLogsWithFailureAndError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    oneAfterAnotherTestTemplate(testContext, vertx,
        CircuitBreakerDoActions::applyFailure,CircuitBreakerDoActions::applyErrorTransition,
        result -> {
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(2, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());

            ActionInvocationLog invocationLog2 = doActionsLogs.get(1);
            Assertions.assertFalse(invocationLog2.isSuccess());
            Assertions.assertNotNull(invocationLog2.getDoActionLog());
          });
          testContext.completeNow();
        });
  }

  @Test
  @DisplayName("Expect fallback when first call ends up with failure and second with timeout.")
  void expectFallbackActionLogsWithErrorAndTimeout(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    oneAfterAnotherTestTemplate(testContext, vertx,
        CircuitBreakerDoActions::applyFailure,CircuitBreakerDoActions.applyTimeout(vertx),
        result -> {
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            System.out.println(actionLog.toJson());
            List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(1, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        });
  }

  @Test
  @DisplayName("Expect fallback when first call ends up with timeout and second with failure.")
  void expectFallbackActionLogsWithTimeoutAndError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    oneAfterAnotherTestTemplate(testContext, vertx,
        CircuitBreakerDoActions::applyFailure,CircuitBreakerDoActions.applyTimeout(vertx),
        result -> {
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            System.out.println(actionLog.toJson());
            List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(1, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        });
  }

  @Test
  @DisplayName("Expect fallback when both calls ends up with timeout.")
  void expectFallbackActionLogsWithTimeouts(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    oneAfterAnotherTestTemplate(testContext, vertx,
        CircuitBreakerDoActions::applyFailure,CircuitBreakerDoActions.applyTimeout(vertx),
        result -> {
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getDoActionLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(1, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        });
  }


  private void oneAfterAnotherTestTemplate(VertxTestContext testContext, Vertx vertx, Action first,
      Action second, Handler<AsyncResult<FragmentResult>> handler) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setFallbackOnFailure(true)
        .setMaxRetries(1).setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions.applyOneAfterAnother(first, second),
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()), handler);

    //then
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}