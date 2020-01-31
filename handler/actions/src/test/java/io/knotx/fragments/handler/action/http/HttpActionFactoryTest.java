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
package io.knotx.fragments.handler.action.http;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class HttpActionFactoryTest {

  @Test
  @DisplayName("Expect exception when doAction provided")
  void expectExceptionWhenDoActionProvided(Vertx vertx) throws Throwable {
    HttpActionFactory actionFactory = new HttpActionFactory();
    JsonObject config = new JsonObject();
    assertThrows(IllegalArgumentException.class, () -> {
      actionFactory.create("", config, vertx, (fragmentContext, resultHandler) -> {
      });
    });
  }

  @Test
  @DisplayName("Should create http action when properly configured")
  void shouldCreateHttpActionWhenProperlyConfigured(Vertx vertx) {
    HttpActionFactory actionFactory = new HttpActionFactory();
    JsonObject config = new JsonObject();
    assertTrue(actionFactory.create("", config, vertx, null) instanceof HttpAction);
  }
}
