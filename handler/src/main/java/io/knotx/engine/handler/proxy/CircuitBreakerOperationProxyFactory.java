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
package io.knotx.engine.handler.proxy;

import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.engine.api.proxy.FragmentOperation;
import io.knotx.engine.api.proxy.OperationProxyFactory;
import io.knotx.fragment.Fragment;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;

public class CircuitBreakerOperationProxyFactory implements OperationProxyFactory {

  @Override
  public String getName() {
    return "cb";
  }

  @Override
  public FragmentOperation create(String alias, JsonObject config,
      Optional<FragmentOperation> proxy,
      Vertx vertx) {

    String circuitBreakerName = config.getString("circuitBreakerName");
    JsonObject circuitBreakerOptions = config.getJsonObject("circuitBreakerOptions");
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl(circuitBreakerName, vertx,
        new CircuitBreakerOptions(circuitBreakerOptions));

    FragmentOperation nextProxy = proxy.orElseThrow(
        () -> new IllegalStateException("Circuit Breaker proxy requires operation to follow"));

    return (fragmentContext, resultHandler) -> circuitBreaker.executeWithFallback(
        f -> nextProxy.apply(fragmentContext,
            knotResult -> {
              if (knotResult.succeeded()) {
                f.complete(knotResult.result());
              } else {
                f.fail(knotResult.cause());
              }
            }),
        v -> {
          Fragment fragment = fragmentContext.getFragment();
          return new FragmentResult(fragment, "fallback");
        }
    ).setHandler(resultHandler);
  }
}
