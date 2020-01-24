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

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.fromConfig;
import static java.util.Objects.isNull;

import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.exception.DoActionNotDefinedException;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * This is a factory class creating action, which provides circuit breaker mechanism. It protects
 * the `doAction` action against overloading when it does not respond on time. If t
 */
@Cacheable
public class CircuitBreakerActionFactory implements ActionFactory {

  static final String FALLBACK_TRANSITION = "_fallback";
  static final String FACTORY_NAME = "cb";

  @Override
  public String getName() {
    return FACTORY_NAME;
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    if (isNull(doAction)) {
      throw new DoActionNotDefinedException("Circuit Breaker action requires `doAction` defined");
    }
    CircuitBreakerActionFactoryOptions options = new CircuitBreakerActionFactoryOptions(config);
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl(options.getCircuitBreakerName(), vertx,
        options.getCircuitBreakerOptions());

    return new CircuitBreakerAction(circuitBreaker, doAction, alias,
        fromConfig(options.getLogLevel()), options.getErrorTransitions());
  }
}
