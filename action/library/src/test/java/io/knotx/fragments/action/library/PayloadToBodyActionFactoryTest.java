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
package io.knotx.fragments.action.library;

import static io.knotx.fragments.action.library.TestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.TestUtils.doActionIdle;
import static io.knotx.fragments.action.library.TestUtils.someFragmentWithPayload;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
public class PayloadToBodyActionFactoryTest {

  private static final String PAYLOAD_KEY = "key";

  private static final JsonObject USER = new JsonObject().put("name", "kovalsky");
  private static final JsonObject NESTED_PAYLOAD = new JsonObject().put("user", USER);
  private static final JsonObject PAYLOAD = new JsonObject().put("key", NESTED_PAYLOAD);

  private PayloadToBodyActionFactory tested;

  @BeforeEach
  void setUp() {
    tested = new PayloadToBodyActionFactory();
  }

  @Test
  @DisplayName("Expect IllegalArgumentException when doAction specified.")
  void createActionWithDoAction() {
    // given
    JsonObject config = new JsonObject()
        .put(PAYLOAD_KEY, "key");

    // when, then
    assertThrows(IllegalArgumentException.class,
        () -> tested.create(ACTION_ALIAS, config, null, doActionIdle()));
  }

  @Test
  @DisplayName("Expect body with nested payload under paylod key.")
  void applyActionWithActionAlias(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put(PAYLOAD_KEY, "key");

    Action action = tested.create(ACTION_ALIAS, config, null, null);

    // when, then
    verifyActionResult(testContext, action, someFragmentWithPayload(PAYLOAD), result -> {
      assertTrue(result.succeeded());
      String body = result.result().getFragment().getBody();
      assertEquals(NESTED_PAYLOAD, new JsonObject(body));
    });
  }

  @Test
  @DisplayName("Expect body with user payload under payload key.user.")
  void applyActionWithNestedKey(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put(PAYLOAD_KEY, "key.user");

    Action action = tested.create(ACTION_ALIAS, config, null, null);

    // when, then
    verifyActionResult(testContext, action, someFragmentWithPayload(PAYLOAD), result -> {
      assertTrue(result.succeeded());
      String body = result.result().getFragment().getBody();
      assertEquals(USER, new JsonObject(body));
    });
  }
}
