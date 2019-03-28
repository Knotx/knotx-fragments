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
package io.knotx.engine.handler.action;

import io.knotx.engine.api.fragment.Action;
import io.knotx.engine.api.fragment.ActionFactory;
import io.knotx.engine.api.fragment.CacheableAction;
import io.knotx.engine.api.fragment.FragmentContext;
import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.engine.handler.exception.DoActionNotDefinedException;
import io.knotx.fragment.Fragment;
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
@CacheableAction
public class CircuitBreakerActionFactory implements ActionFactory {

  static final String FALLBACK_TRANSITION = "fallback";

  @Override
  public String getName() {
    return "cb";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    if (doAction == null) {
      throw new DoActionNotDefinedException("Circuit Breaker action requires `doAction` defined");
    }
    String circuitBreakerName = config.getString("circuitBreakerName");
    CircuitBreakerOptions circuitBreakerOptions =
        config.getJsonObject("circuitBreakerOptions") == null ? new CircuitBreakerOptions()
            : new CircuitBreakerOptions(config.getJsonObject("circuitBreakerOptions"));
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl(circuitBreakerName, vertx,
        circuitBreakerOptions);

    return new CircuitBreakerAction(circuitBreaker, doAction);
  }

  public static class CircuitBreakerAction implements Action {

    private CircuitBreaker circuitBreaker;
    private Action doAction;

    CircuitBreakerAction(CircuitBreaker circuitBreaker, Action doAction) {
      this.circuitBreaker = circuitBreaker;
      this.doAction = doAction;
    }

    @Override
    public void apply(FragmentContext fragmentContext,
        Handler<AsyncResult<FragmentResult>> resultHandler) {
      circuitBreaker.executeWithFallback(
          f -> doAction.apply(fragmentContext,
              result -> {
                if (result.succeeded()) {
                  f.complete(result.result());
                } else {
                  f.fail(result.cause());
                }
              }),
          v -> {
            Fragment fragment = fragmentContext.getFragment();
            return new FragmentResult(fragment, FALLBACK_TRANSITION);
          }
      ).setHandler(resultHandler);
    }
  }
}
