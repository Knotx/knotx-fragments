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
package io.knotx.fragments.action.library.cb;

import static io.knotx.fragments.action.library.cb.CircuitBreakerActionFactory.FALLBACK_TRANSITION;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Instant.now;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.action.library.exception.DoActionExecuteException;
import io.knotx.fragments.action.library.helper.TimeCalculator;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.log.ActionLog;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class CircuitBreakerAction implements Action {

  static final String INVOCATION_COUNT_LOG_KEY = "invocationCount";
  static final String ERROR_LOG_KEY = "error";

  private final String alias;
  private final CircuitBreaker circuitBreaker;
  private final Action doAction;
  private final Set<String> errorTransitions;
  private final ActionLogLevel actionLogLevel;


  CircuitBreakerAction(CircuitBreaker circuitBreaker, Action doAction, String alias,
      ActionLogLevel actionLogLevel, Set<String> errorTransitions) {
    this.alias = alias;
    this.circuitBreaker = circuitBreaker;
    this.doAction = doAction;
    this.errorTransitions = errorTransitions;
    this.actionLogLevel = actionLogLevel;
  }

  @Override
  public void apply(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    AtomicInteger counter = new AtomicInteger();
    ActionLogger actionLogger = ActionLogger.create(alias, actionLogLevel);
    circuitBreaker.executeWithFallback(
        promise -> executeCommand(promise, fragmentContext, counter, actionLogger),
        throwable -> handleFallback(fragmentContext, throwable, counter, actionLogger)
    ).setHandler(resultHandler);
  }

  private void executeCommand(Promise<FragmentResult> promise, FragmentContext fragmentContext,
      AtomicInteger counter, ActionLogger actionLogger) {
    counter.incrementAndGet();
    long startTime = now().toEpochMilli();
    try {
      doAction.apply(fragmentContext,
          result -> {
            if (result.succeeded()) {
              handleResult(promise, result.result(), counter, startTime, actionLogger);
            } else {
              handleFail(promise, null, startTime, result.cause(), actionLogger);
            }
          });
    } catch (Exception e) {
      handleFail(promise, null, startTime, e, actionLogger);
      throw e;
    }
  }

  private void handleResult(Promise<FragmentResult> promise,
      FragmentResult result, AtomicInteger counter, long startTime,
      ActionLogger actionLogger) {
    if (isErrorTransition(result)) {
      handleFail(promise, result.getLog(), startTime,
          new DoActionExecuteException(
              format("Action end up %s transition", result.getTransition())),
          actionLogger);
    } else {
      handleSuccess(promise, result, counter, startTime, actionLogger);
    }
  }

  private boolean isErrorTransition(FragmentResult result) {
    return errorTransitions.contains(result.getTransition());
  }

  private static void handleFail(Promise<FragmentResult> promise, JsonObject nodeLog,
      long startTime, Throwable error, ActionLogger actionLogger) {
    actionLogger.failureDoActionLog(TimeCalculator.executionTime(startTime), nodeLog);
    promise.fail(error);
  }

  private static void handleSuccess(Promise<FragmentResult> f, FragmentResult result,
      AtomicInteger counter, long startTime, ActionLogger actionLogger) {
    actionLogger.info(INVOCATION_COUNT_LOG_KEY, valueOf(counter.get()));
    actionLogger.doActionLog(TimeCalculator.executionTime(startTime), result.getLog());
    f.complete(new FragmentResult(result.getFragment(), result.getTransition(),
        actionLogger.toLog().toJson()));
  }

  private static FragmentResult handleFallback(FragmentContext fragmentContext,
      Throwable throwable,
      AtomicInteger counter, ActionLogger actionLogger) {
    logFallback(throwable, counter, actionLogger);
    Fragment fragment = fragmentContext.getFragment();
    ActionLog actionLog = actionLogger.toLog();
    return new FragmentResult(fragment, FALLBACK_TRANSITION, actionLog.toJson());
  }


  private static void logFallback(Throwable throwable, AtomicInteger counter,
      ActionLogger actionLogger) {
    actionLogger.error(INVOCATION_COUNT_LOG_KEY, valueOf(counter.get()));
    actionLogger
        .error(ERROR_LOG_KEY,
            format("Exception: %s. %s", throwable.getClass(), throwable.getLocalizedMessage()));
  }
}
