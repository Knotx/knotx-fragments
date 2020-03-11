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
package io.knotx.fragments.handler.action.cb;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.action.cb.CircuitBreakerAction.ERROR_LOG_KEY;
import static io.knotx.fragments.handler.action.cb.CircuitBreakerAction.INVOCATION_COUNT_LOG_KEY;
import static io.knotx.fragments.handler.action.cb.CircuitBreakerActionFactory.FALLBACK_TRANSITION;
import static io.knotx.fragments.handler.action.cb.CircuitBreakerDoActions.CUSTOM_TRANSITION;
import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.INFO;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.action.exception.DoActionExecuteException;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.actionlog.ActionInvocationLog;
import io.knotx.fragments.handler.api.actionlog.ActionLog;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
class CircuitBreakerActionFactoryTest {

  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(), "expectedBody");
  private static final int TIMEOUT_IN_MS = 500;
  public static final int NO_RETRY = 0;

  @Test
  @DisplayName("Expect factory name is 'cb'.")
  void checkFactoryName() {
    // given
    assertEquals(CircuitBreakerActionFactory.FACTORY_NAME,
        new CircuitBreakerActionFactory().getName());
  }

  @ParameterizedTest
  @MethodSource("provideSuccessActions")
  @DisplayName("Expect _success transition when action ends with success.")
  void expectSuccessWhenSuccess(Action action, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                assertEquals(SUCCESS_TRANSITION, result.getTransition());
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideErrorActionsAndTimeout")
  @DisplayName("Expect _fallback transition when action ends with error.")
  void expectFallbackWhenError(Action action, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                assertEquals(FALLBACK_TRANSITION, result.getTransition());
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideSuccessActions")
  @DisplayName("Expect empty node log when action ends with success and default log level.")
  void expectEmptyNodeLogWhenSuccess(Action action, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                assertTrue(actionLog.getLogs().isEmpty());
                assertTrue(actionLog.getInvocationLogs().isEmpty());
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideAllActions")
  @DisplayName("Expect invocation count when log level is info.")
  void expectInvocationCountWhenInfo(Action action, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Action tested = newInstanceWithInfo(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                assertEquals("1", actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY));
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideSuccessActions")
  @DisplayName("Expect success invocation log is added when action ends with success and log level is info.")
  void expectSuccessInvocationLogWhenSuccessAndInfo(Action action, VertxTestContext testContext,
      Vertx vertx)
      throws Throwable {
    // given
    Action tested = newInstanceWithInfo(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                assertEquals(1, actionLog.getInvocationLogs().size());
                assertTrue(actionLog.getInvocationLogs().iterator().next().isSuccess());
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideErrorActions")
  @DisplayName("Expect error invocation log is added when action ends with error.")
  void expectErrorInvocationLogWhenErrorAndInfo(Action action, VertxTestContext testContext,
      Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                assertEquals(1, actionLog.getInvocationLogs().size());
                assertFalse(actionLog.getInvocationLogs().iterator().next().isSuccess());
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideErrorActions")
  @DisplayName("Expect two invocation logs are added when action ends always with error and retry is 1.")
  void expectTwoInvocationLogsWhenErrorAndRetry(Action action, VertxTestContext testContext,
      Vertx vertx)
      throws Throwable {
    // given
    Action tested = newInstanceWithRetry(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                assertEquals("2", actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY));
                assertEquals(2, actionLog.getInvocationLogs().size());
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect DoActionExecuteException in error message when action ends with _error transition.")
  void expectDoActionExecuteException(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(CircuitBreakerDoActions::applyErrorTransition, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                String errorMessage = actionLog.getLogs().getString(ERROR_LOG_KEY);
                assertNotNull(errorMessage);
                assertTrue(errorMessage.contains(DoActionExecuteException.class.getName()));
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }


  @ParameterizedTest
  @MethodSource("provideFailureAndExceptionActions")
  @DisplayName("Expect original exception in error message when action throws an exception or fails.")
  void expectIllegalStateExceptionWhenThrowsException(Action action, VertxTestContext testContext,
      Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(action, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                String errorMessage = actionLog.getLogs().getString(ERROR_LOG_KEY);
                assertNotNull(errorMessage);
                assertTrue(errorMessage.contains(IllegalStateException.class.getName()));
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect Timeout exception in error message when action times out.")
  void expectFallbackWhenTimout(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Action tested = newActionInstance(CircuitBreakerDoActions::applySuccessDelay, vertx);

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext
              .verify(() -> {
                //then
                ActionLog actionLog = new ActionLog(result.getLog());
                String errorMessage = actionLog.getLogs().getString(ERROR_LOG_KEY);
                assertNotNull(errorMessage);
                assertTrue(errorMessage.contains(TimeoutException.class.getName()));
              });
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
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
        CircuitBreakerDoActions::applySuccess,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);

            assertEquals(2, doActionsLogs.size());
            assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            assertFalse(invocationLog1.isSuccess());
            assertNull(invocationLog1.getDoActionLog());

            ActionInvocationLog invocationLog2 = doActionsLogs.get(1);
            assertTrue(invocationLog2.isSuccess());
            assertNotNull(invocationLog2.getDoActionLog());
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _success transition when first call ends with _error and second with _success.")
  void expectSuccessWhenErrorThenSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(CircuitBreakerDoActions::applyErrorTransition,
        CircuitBreakerDoActions::applySuccess, result -> {
          //then
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);

            assertEquals(2, doActionsLogs.size());
            assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            assertFalse(invocationLog1.isSuccess());
            assertNotNull(invocationLog1.getDoActionLog());

            ActionInvocationLog invocationLog2 = doActionsLogs.get(1);
            assertTrue(invocationLog2.isSuccess());
            assertNotNull(invocationLog2.getDoActionLog());

          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _fallback transition when first call fails and second ends with _error.")
  void expectFallbackWhenFailureThenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyFailure,
        CircuitBreakerDoActions::applyErrorTransition,
        result -> {
          //then
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);

            assertEquals(2, doActionsLogs.size());
            assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            assertFalse(invocationLog1.isSuccess());
            assertNull(invocationLog1.getDoActionLog());

            ActionInvocationLog invocationLog2 = doActionsLogs.get(1);
            assertFalse(invocationLog2.isSuccess());
            assertNotNull(invocationLog2.getDoActionLog());
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _fallback transition when first call fails and second times out.")
  void expectFallbackWhenFailureThenTimeout(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyFailure,
        CircuitBreakerDoActions::applySuccessDelay,
        result -> {
          //then
          testContext.verify(() -> {
            FragmentResult fragmentResult = result.result();
            assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);
            assertEquals(1, doActionsLogs.size());
            assertEquals("2", invocationCount);
            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            assertFalse(invocationLog1.isSuccess());
            assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _fallback transition when first call times out and second fails.")
  void expectFallbackWhenTimeoutThenFailure(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applySuccessDelay,
        CircuitBreakerDoActions::applyFailure,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);

            assertEquals(1, doActionsLogs.size());
            assertEquals("2", invocationCount);

            ActionInvocationLog invocationLog1 = doActionsLogs.get(0);
            assertFalse(invocationLog1.isSuccess());
            assertNull(invocationLog1.getDoActionLog());
          });
          testContext.completeNow();
        }, testContext, vertx
    );
  }

  @Test
  @DisplayName("Expect _fallback transition when all calls time out.")
  void expectFallbackWhenTimeouts(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applySuccessDelay,
        CircuitBreakerDoActions::applySuccessDelay,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);
            assertEquals(0, doActionsLogs.size());
            assertEquals("2", invocationCount);

            String errorMessage = actionLog.getLogs().getString(ERROR_LOG_KEY);
            assertNotNull(errorMessage);
            assertTrue(errorMessage.contains("TimeoutException"));
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
        CircuitBreakerDoActions::applySuccessDelay,
        CircuitBreakerDoActions::applySuccess,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);
            assertEquals(1, doActionsLogs.size());
            assertEquals("2", invocationCount);
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _fallback transitions when both calls end with exception.")
  void expectFallbackWhenExceptions(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    validateScenario(
        CircuitBreakerDoActions::applyException,
        CircuitBreakerDoActions::applyException,
        result -> {
          testContext.verify(() -> {
            //then
            FragmentResult fragmentResult = result.result();
            assertEquals(FALLBACK_TRANSITION, fragmentResult.getTransition());

            ActionLog actionLog = new ActionLog(fragmentResult.getLog());
            List<ActionInvocationLog> doActionsLogs = actionLog.getInvocationLogs();
            String invocationCount = actionLog.getLogs().getString(INVOCATION_COUNT_LOG_KEY);
            assertEquals(2, doActionsLogs.size());
            assertEquals("2", invocationCount);
          });
          testContext.completeNow();
        }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect _fallback transition when action ends with custom transition that means error")
  void shouldEndWithFallbackTransitionWhenCustomErrorTransitionsProvided(
      VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    //given
    Set<String> errorTransitions = Collections.singleton(CUSTOM_TRANSITION);
    Action tested = newInstanceWithErrorTransitions(CircuitBreakerDoActions::applyCustomTransition,
        vertx, errorTransitions, NO_RETRY);

    //when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext.verify(() -> assertEquals(FALLBACK_TRANSITION, result.getTransition()));
          testContext.completeNow();
        }));
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect custom transition when action ends with custom transition.")
  void shouldEndWithSuccessAfterCustom(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    //given
    Action tested = newInstanceWithErrorTransitions(CircuitBreakerDoActions::applyCustomTransition,
        vertx, Collections.emptySet(), NO_RETRY);

    //when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          assertEquals(CUSTOM_TRANSITION, result.getTransition());
          testContext.completeNow();
        }));
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect _fallback transition when all invocations end with custom transitions that mean errors.")
  void shouldEndWithFallbackWhenRetryEndsWithCustom(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    //given
    Set<String> errorTransitions = new HashSet<>();
    errorTransitions.add(CUSTOM_TRANSITION);
    Action tested = newInstanceWithErrorTransitions(CircuitBreakerDoActions
        .applyOneAfterAnother(CircuitBreakerDoActions::applyCustomTransition,
            CircuitBreakerDoActions::applyCustomTransition), vertx, errorTransitions, 1);
    //when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        testContext.succeeding(result -> {
          testContext.verify(() -> assertEquals(FALLBACK_TRANSITION, result.getTransition()));
          testContext.completeNow();
        }));
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private Action newActionInstance(Action action, Vertx vertx) {
    return new CircuitBreakerActionFactory().create("alias",
        new CircuitBreakerActionFactoryOptions()
            .setCircuitBreakerOptions(new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS))
            .toJson(), vertx, action);
  }

  private Action newInstanceWithErrorTransitions(Action action, Vertx vertx,
      Set<String> errorTransitions, int maxRetries) {
    return new CircuitBreakerActionFactory().create("alias",
        new CircuitBreakerActionFactoryOptions()
            .setCircuitBreakerOptions(
                new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS).setMaxRetries(maxRetries))
            .setErrorTransitions(errorTransitions)
            .toJson(), vertx, action);
  }

  private Action newInstanceWithRetry(Action action, Vertx vertx) {
    return new CircuitBreakerActionFactory().create("alias",
        new CircuitBreakerActionFactoryOptions()
            .setCircuitBreakerOptions(
                new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS).setMaxRetries(1)).toJson(),
        vertx,
        action);
  }

  private Action newInstanceWithInfo(Action action, Vertx vertx) {
    return new CircuitBreakerActionFactory().create("alias",
        new CircuitBreakerActionFactoryOptions()
            .setCircuitBreakerOptions(new CircuitBreakerOptions().setTimeout(TIMEOUT_IN_MS))
            .setLogLevel(INFO.getLevel()).toJson(),
        vertx,
        action);
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
            .applyOneAfterAnother(firstInvocationBehaviour, secondInvocationBehaviour), "tested",
        INFO, Collections.singleton(ERROR_TRANSITION));

    // when
    tested.apply(new FragmentContext(FRAGMENT, new ClientRequest()), handler);

    // validate if finished
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private static Stream<Action> provideAllActions() {
    return Stream.concat(provideSuccessActions(), provideErrorActionsAndTimeout());
  }

  private static Stream<Action> provideSuccessActions() {
    return Stream.of(
        CircuitBreakerDoActions::applySuccess
    );
  }

  private static Stream<Action> provideErrorActionsAndTimeout() {
    return Stream.concat(
        provideErrorActions(),
        Stream.of(CircuitBreakerDoActions::applySuccessDelay)
    );
  }

  private static Stream<Action> provideErrorActions() {
    return Stream.concat(
        provideFailureAndExceptionActions(),
        Stream
            .of(CircuitBreakerDoActions::applyException)
    );
  }

  private static Stream<Action> provideFailureAndExceptionActions() {
    return Stream.of(
        CircuitBreakerDoActions::applyFailure,
        CircuitBreakerDoActions::applyException
    );
  }
}
