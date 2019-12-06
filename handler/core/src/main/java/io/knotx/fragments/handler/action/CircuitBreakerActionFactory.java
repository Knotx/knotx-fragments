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
package io.knotx.fragments.handler.action;

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.fromConfig;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Objects.isNull;

import java.util.concurrent.atomic.AtomicInteger;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.actionlog.ActionLog;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.exception.DoActionExecuteException;
import io.knotx.fragments.handler.exception.DoActionNotDefinedException;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * This is a factory class creating action, which provides circuit breaker mechanism. It protects
 * the `doAction` action against overloading when it does not respond on time. If t
 */
@Cacheable
public class CircuitBreakerActionFactory implements ActionFactory {

  static final String FALLBACK_TRANSITION = "fallback";

  @Override
  public String getName() {
    return "cb";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    if (isNull(doAction)) {
      throw new DoActionNotDefinedException("Circuit Breaker action requires `doAction` defined");
    }
    String circuitBreakerName = config.getString("circuitBreakerName");
    String errorTransition = config.getString("errorTransition", ERROR_TRANSITION);
    CircuitBreakerOptions circuitBreakerOptions =
        config.getJsonObject("circuitBreakerOptions") == null ? new CircuitBreakerOptions()
            : new CircuitBreakerOptions(config.getJsonObject("circuitBreakerOptions"));
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl(circuitBreakerName, vertx,
        circuitBreakerOptions);

    return new CircuitBreakerAction(circuitBreaker, doAction, alias, fromConfig(config),
        errorTransition);
  }

  public static class CircuitBreakerAction implements Action {

    private final CircuitBreaker circuitBreaker;
    private final Action doAction;
    private final ActionLogLevel actionLogLevel;
    private final String alias;
    private final String errorTransition;

    CircuitBreakerAction(CircuitBreaker circuitBreaker, Action doAction, String alias,
        ActionLogLevel actionLogLevel, String errorTransition) {
      this.circuitBreaker = circuitBreaker;
      this.doAction = doAction;
      this.alias = alias;
      this.actionLogLevel = actionLogLevel;
      this.errorTransition = errorTransition;
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
      doAction.apply(fragmentContext,
          result -> {
            if (result.succeeded()) {
              handleResult(promise, result.result(), counter, startTime, actionLogger);
            } else {
              handleFail(promise, null, startTime, "Action failed", actionLogger);
            }
          });
    }

    private void handleResult(Promise<FragmentResult> promise,
        FragmentResult result, AtomicInteger counter,  long startTime,
        ActionLogger actionLogger) {
      if (isErrorTransition(result)) {
        handleFail(promise, result.getNodeLog(), startTime,
            format("Action end up %s transition", errorTransition), actionLogger);
      } else {
        handleSuccess(promise, result, counter, startTime, actionLogger);
      }
    }

    private boolean isErrorTransition(FragmentResult result) {
      return ERROR_TRANSITION.equals(result.getTransition()) || errorTransition
          .equals(result.getTransition());
    }

    private static void handleFail(Promise<FragmentResult> promise, JsonObject nodeLog,
        long startTime, String error, ActionLogger actionLogger) {
      actionLogger.failureDoActionLog(executionTime(startTime), nodeLog);
      promise.fail(new DoActionExecuteException(error));
    }

    private static long executionTime(long startTime) {
      return now().toEpochMilli() - startTime;
    }

    private static void handleSuccess(Promise<FragmentResult> f, FragmentResult result,
        AtomicInteger counter, long startTime, ActionLogger actionLogger) {
      actionLogger.info("invocationCount", valueOf(counter.get()));
      actionLogger.doActionLog(executionTime(startTime), result.getNodeLog());
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
      actionLogger.info("invocationCount", valueOf(counter.get()));
      actionLogger
          .error("fallback",
              format("Exception: %s. %s", throwable.getClass(), throwable.getLocalizedMessage()));
    }
  }
}