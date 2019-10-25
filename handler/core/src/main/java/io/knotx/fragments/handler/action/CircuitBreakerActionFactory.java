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

import static java.lang.String.format;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionConfig;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.exception.DoActionNotDefinedException;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
  public Action create(ActionConfig config, Vertx vertx) {
    if (!config.hasAction()) {
      throw new DoActionNotDefinedException("Circuit Breaker action requires `doAction` defined");
    }
    JsonObject options = config.getOptions();
    String circuitBreakerName = options.getString("circuitBreakerName");
    CircuitBreakerOptions circuitBreakerOptions =
        options.getJsonObject("circuitBreakerOptions") == null ? new CircuitBreakerOptions()
            : new CircuitBreakerOptions(options.getJsonObject("circuitBreakerOptions"));
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl(circuitBreakerName, vertx,
        circuitBreakerOptions);

    return new CircuitBreakerAction(circuitBreaker, config.getDoAction(),
        ActionLogger.create(config));
  }

  public static class CircuitBreakerAction implements Action {

    private CircuitBreaker circuitBreaker;
    private Action doAction;
    private ActionLogger actionLogger;

    CircuitBreakerAction(CircuitBreaker circuitBreaker, Action doAction,
        ActionLogger actionLogger) {
      this.circuitBreaker = circuitBreaker;
      this.doAction = doAction;
      this.actionLogger = actionLogger;
    }

    @Override
    public void apply(FragmentContext fragmentContext,
        Handler<AsyncResult<FragmentResult>> resultHandler) {
      circuitBreaker.executeWithFallback(
          f -> doAction.apply(fragmentContext,
              result -> {
                if (result.succeeded()) {
                  FragmentResult fr = result.result();
                  actionLogger.doActionLog(fr.getActionLog());
                  f.complete(new FragmentResult(fr.getFragment(), fr.getTransition(), actionLogger.toLog()));
                } else {
                  this.actionLogger.error( "Failed to perform doAction");
                  f.fail(result.cause());
                }
              }),
          v -> {
            Fragment fragment = fragmentContext.getFragment();
            actionLogger.error("fallback", format("Cannot process doAction. Exit with %s", FALLBACK_TRANSITION));
            return new FragmentResult(fragment, FALLBACK_TRANSITION, actionLogger.toLog());
          }
      ).setHandler(resultHandler);
    }
  }
}