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

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

class CircuitBreakerActionFactoryOptionsTest {

  @DisplayName("Expect fallback on failure always enabled")
  @Test
  void expectFallbackOnFailure() {
    //given
    CircuitBreakerOptions options = new CircuitBreakerOptions().setFallbackOnFailure(false);
    JsonObject json = new CircuitBreakerActionFactoryOptions()
        .setCircuitBreakerOptions(options).toJson();

    // when
    CircuitBreakerActionFactoryOptions tested = new CircuitBreakerActionFactoryOptions(json);

    // then
    assertTrue(tested.getCircuitBreakerOptions().isFallbackOnFailure());
  }


  @DisplayName("Expect random circuit breaker name when not defined.")
  @Test
  void expectCircuitBreakerNameNotBlank() {
    //given
    JsonObject json = new CircuitBreakerActionFactoryOptions().toJson();

    // when
    CircuitBreakerActionFactoryOptions tested = new CircuitBreakerActionFactoryOptions(json);

    // then
    assertNotNull(tested.getCircuitBreakerName());
    assertTrue(StringUtils.isNotBlank(tested.getCircuitBreakerName()));
  }

  @DisplayName("Expect _error transition is added to error transition set.")
  @Test
  void expectErrorTransitionConfigured() {
    //given
    Set<String> errorTransitions = new HashSet<>();
    JsonObject json = new CircuitBreakerActionFactoryOptions()
        .setErrorTransitions(errorTransitions).toJson();

    // when
    CircuitBreakerActionFactoryOptions tested = new CircuitBreakerActionFactoryOptions(json);

    // then
    assertTrue(tested.getErrorTransitions().contains(ERROR_TRANSITION));
  }
}