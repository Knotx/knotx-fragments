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
package io.knotx.fragments.action.library.http;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.knotx.fragments.action.api.Cacheable;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(VertxExtension.class)
class HttpActionFactoryTest {

  @Test
  @DisplayName("Expect exception when doAction provided")
  void expectExceptionWhenDoActionProvided(Vertx vertx) {
    HttpActionFactory actionFactory = new HttpActionFactory();
    JsonObject config = new JsonObject();
    assertThrows(ActionConfigurationException.class,
        () -> actionFactory.create("", config, vertx, (fragmentContext, resultHandler) -> {})
    );
  }

  @Test
  @DisplayName("Should create http action when properly configured")
  void shouldCreateHttpActionWhenProperlyConfigured(Vertx vertx) {
    HttpActionFactory actionFactory = new HttpActionFactory();
    JsonObject config = new JsonObject();
    assertTrue(actionFactory.create("", config, vertx, null) instanceof HttpAction);
  }

  @Test
  @DisplayName("Http Action is stateless and should be cached.")
  void shouldBeCacheable() {
    Class<?> tested = HttpActionFactory.class;
    Assertions.assertNotNull(tested.getAnnotation(Cacheable.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"})
  @DisplayName("Http Action supports expected methods")
  void shouldSupportMethods(String method, Vertx vertx) {
    HttpActionFactory tested = new HttpActionFactory();
    JsonObject config = new JsonObject().put("httpMethod", method);
    assertTrue(tested.create("", config, vertx, null) instanceof HttpAction);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CONNECT", "OPTIONS", "TRACE"})
  @DisplayName("Http Action Factory throws when not supported HTTP method specified")
  void shouldThrowOnNotSupportedMethods(String method, Vertx vertx) {
    HttpActionFactory tested = new HttpActionFactory();
    JsonObject config = new JsonObject().put("httpMethod", method);
    assertThrows(ActionConfigurationException.class, () -> tested.create("", config, vertx, null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "getpost", "postt"})
  @DisplayName("Http Action Factory throws when invalid HTTP method specified")
  void shouldThrowOnInvalidMethods(String method, Vertx vertx) {
    HttpActionFactory tested = new HttpActionFactory();
    JsonObject config = new JsonObject().put("httpMethod", method);
    assertThrows(ActionConfigurationException.class, () -> tested.create("", config, vertx, null));
  }
}
