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
  @DisplayName("Expect failure with timeout exception when fallback is not enabled and doAction times out.")
  void expectFailureWhenFallbackDisabledAndDoActionTimesOut(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions.applySuccessDelay(vertx),
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.failing(result -> {
          testContext
              .verify(() -> {
                //then
                Assertions.assertTrue(result instanceof TimeoutException);
              });
          testContext.completeNow();
        }));

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect _success transition when doAction ends with _success transition.")
  void expectSuccessWhenSuccess(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setFallbackOnFailure(true);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applySuccessWithActionLogs,
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                Assertions.assertEquals(SUCCESS_TRANSITION, result.getTransition());

                ActionLog actionLog = new ActionLog(result.getNodeLog());
                List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
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

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect fallback transition when doAction times out.")
  void expectFallbackWhenTimout(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setFallbackOnFailure(true)
        .setMaxRetries(1)
        .setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);
    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions.applySuccessDelay(vertx),
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                Assertions.assertEquals(FALLBACK_TRANSITION, result.getTransition());

                ActionLog actionLog = new ActionLog(result.getNodeLog());
                String fallback = actionLog.getLogs().getString("fallback");
                String invocationCount = actionLog.getLogs().getString("invocationCount");

                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("TimeoutException"));
                Assertions.assertEquals("2", invocationCount);
              });
          testContext.completeNow();
        }));

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect fallback transition when doAction ends with _error transition.")
  void expectFallbackWhenError(VertxTestContext testContext, Vertx vertx)
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
                //then
                Assertions.assertEquals(FALLBACK_TRANSITION, result.getTransition());

                ActionLog actionLog = new ActionLog(result.getNodeLog());
                String fallback = actionLog.getLogs().getString("fallback");
                List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();

                Assertions.assertEquals(1, doActionsLogs.size());
                ActionInvocationLog invocationLog = doActionsLogs.get(0);
                Assertions.assertFalse(invocationLog.isSuccess());
                Assertions.assertNotNull(invocationLog.getDoActionLog());
                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("DoActionExecuteException"));
              });
          testContext.completeNow();
        }));

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect fallback transition when doAction fails.")
  void expectFallbackWhenFailure(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setFallbackOnFailure(true);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions::applyFailure, "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          testContext
              .verify(() -> {
                //then
                Assertions.assertEquals(FALLBACK_TRANSITION, result.result().getTransition());

                ActionLog actionLog = new ActionLog(result.result().getNodeLog());
                List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
                Assertions.assertEquals(1, doActionsLogs.size());
                ActionInvocationLog invocationLog = doActionsLogs.get(0);

                Assertions.assertFalse(invocationLog.isSuccess());
                Assertions.assertNull(invocationLog.getDoActionLog());

                String fallback = actionLog.getLogs().getString("fallback");
                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("DoActionExecuteException"));
              });
          testContext.completeNow();
        });

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect fallback transition when doAction throws exception.")
  void expectFallbackWhenException(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setFallbackOnFailure(true);
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

                String invocationCount = actionLog.getLogs().getString("invocationCount");
                Assertions.assertEquals("1", invocationCount);

                String fallback = actionLog.getLogs().getString("fallback");
                Assertions.assertNotNull(fallback);
                Assertions.assertTrue(fallback.contains("ReplyException"));

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
  @DisplayName("Expect _success transition when first invocation fails and second ends with _success.")
  void expectSuccessWhenFailureThenSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyFailure,
        CircuitBreakerDoActions::applySuccessWithActionLogs,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
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
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _success transition when first call ends with _error and second with _success.")
  void expectSuccessWhenErrorThenSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(CircuitBreakerDoActions::applyErrorTransition,
        CircuitBreakerDoActions::applySuccessWithActionLogs, result -> {
          //then
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
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
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect fallback transition when first call fails and second ends with _error.")
  void expectFallbackWhenFailureThenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyFailure,
        CircuitBreakerDoActions::applyErrorTransition,
        result -> {
          //then
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
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
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect fallback transition when first call fails and second times out.")
  void expectFallbackWhenFailureThenTimeout(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyFailure,
        CircuitBreakerDoActions.applySuccessDelay(vertx),
        result -> {
          //then
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");
            Assertions.assertEquals(1, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);
            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect fallback transition when first call times out and second fails.")
  void expectFallbackWhenTimeoutThenFailure(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions.applySuccessDelay(vertx),
        CircuitBreakerDoActions::applyFailure,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");

            Assertions.assertEquals(1, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            Assertions.assertFalse(invocationLog1.isSuccess());
            Assertions.assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        }, testContext, vertx
    );
  }

  @Test
  @DisplayName("Expect fallback transition when both calls time out.")
  void expectFallbackWhenTimeouts(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(CircuitBreakerDoActions.applySuccessDelay(vertx),
        CircuitBreakerDoActions.applySuccessDelay(vertx), result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");
            Assertions.assertEquals(0, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);
          });
          testContext.completeNow();
        }, testContext, vertx
    );
  }

  @Test
  @DisplayName("Expect _success transition when first call times out and second ends with _success.")
  void expectSuccessWhenTimeoutThenSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions.applySuccessDelay(vertx, 800),
        CircuitBreakerDoActions::applySuccessWithActionLogs,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");
            Assertions.assertEquals(1, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect fallback transitions when both calls end with exception.")
  void expectFallbackWhenExceptions(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyException,
        CircuitBreakerDoActions::applyException,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            Assertions.assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getNodeLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString("invocationCount");
            Assertions.assertEquals(0, doActionsLogs.size());
            Assertions.assertEquals("2", invocationCount);
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  private void validateScenario(Action firstInvocationBehaviour, Action secondInvocationBehaviour,
      Handler<AsyncResult<FragmentResult>> handler, VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given
    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setFallbackOnFailure(true)
        .setMaxRetries(1)
        .setTimeout(TIMEOUT_IN_MS);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx, options);

    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        CircuitBreakerDoActions
            .applyOneAfterAnother(firstInvocationBehaviour, secondInvocationBehaviour),
        "tested", INFO, ERROR_TRANSITION);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()), handler);

    // validate if finished
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}