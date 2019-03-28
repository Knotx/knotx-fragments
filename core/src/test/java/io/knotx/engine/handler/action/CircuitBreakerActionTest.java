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

import static io.knotx.engine.api.fragment.FragmentResult.DEFAULT_TRANSITION;

import io.knotx.engine.api.fragment.Action;
import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.engine.handler.action.CircuitBreakerActionFactory.CircuitBreakerAction;
import io.knotx.fragment.Fragment;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.impl.CircuitBreakerImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// TODO
@ExtendWith(VertxExtension.class)
class CircuitBreakerActionTest {

  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(), "expectedBody");
  private static final Action DO_ACTION = (fragmentContext, resultHandler) ->
      new FragmentResult(FRAGMENT, DEFAULT_TRANSITION);


  @Test
  @DisplayName("Expect operation ends when doAction ends on time.")
  void expectOperationEnds(Vertx vertx) throws Throwable {
    // given

  }

  @Test
  @DisplayName("Expect fallback transition when doAction times out.")
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

  @Test
  @DisplayName("Expect doAction applied when circuit is half-opened.")
  void expectDoActionAppliedWhenHalfOpen(Vertx vertx) {
    // given

    // when

    //then
  }


}