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
package io.knotx.engine.handler.action;

import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.engine.handler.action.CircuitBreakerActionFactory.CircuitBreakerAction;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// TODO
@ExtendWith(VertxExtension.class)
class CircuitBreakerActionTest {

  @Test
  @DisplayName("Expect operation ends when doAction ends on time.")
  void expectOperationEnds(Vertx vertx) {
    // given
    CircuitBreaker circuitBreaker = new CircuitBreakerImpl("name", vertx,
        new CircuitBreakerOptions());
    CircuitBreakerAction tested = new CircuitBreakerAction(circuitBreaker,
        (fragmentContext, resultHandler) -> resultHandler.handle(
            Future.succeededFuture(new FragmentResult(null, FragmentResult.DEFAULT_TRANSITION))));
    // TODO
    // when

    //then
  }

  @Test
  @DisplayName("Expect fallback transition when doAction timeouts.")
  void expectFallback(Vertx vertx) {
    // given

    // when

    //then
  }

  @Test
  @DisplayName("Expect doAction not applied when circuit is closed.")
  void expectDoActionNotCalled(Vertx vertx) {
    // given

    // when

    //then
  }


}