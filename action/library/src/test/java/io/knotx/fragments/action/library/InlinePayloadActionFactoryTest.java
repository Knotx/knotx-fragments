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
import static io.knotx.fragments.action.library.TestUtils.someFragment;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.Fragment;
import io.vertx.core.json.JsonArray;
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
class InlinePayloadActionFactoryTest {

  private static final JsonArray EXPECTED_JSON_ARRAY = new JsonArray().add("some value");
  private static final JsonObject EXPECTED_JSON_OBJECT = new JsonObject()
      .put("data", "default value");

  private InlinePayloadActionFactory tested;

  @BeforeEach
  void setUp() {
    tested = new InlinePayloadActionFactory();
  }

  @Test
  @DisplayName("Expect IllegalArgumentException when payload not configured.")
  void createActionWithoutPayload() {
    // when, then
    assertThrows(IllegalArgumentException.class, () -> tested
        .create(ACTION_ALIAS, new JsonObject(), null, null));
  }

  @Test
  @DisplayName("Expect IllegalArgumentException when doAction specified.")
  void createActionWithDoAction() {
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT);

    // when, then
    assertThrows(IllegalArgumentException.class,
        () -> tested.create(ACTION_ALIAS, config, null, doActionIdle()));
  }

  @Test
  @DisplayName("Expect payload with action alias key in Fragment payload when alias not configured.")
  void applyActionWithActionAlias(VertxTestContext testContext) {
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT);

    // given
    Action action = tested.create(ACTION_ALIAS, config, null, null);

    // when, then
    verifyActionResult(testContext, action, result -> assertTrue(
        result.result().getFragment().getPayload().containsKey(ACTION_ALIAS)));
  }

  @Test
  @DisplayName("Expect payload with alias key in Fragment payload when alias configured")
  void applyActionWithAlias(VertxTestContext testContext) {
    // given
    String expectedAlias = "newAction";
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT)
        .put("alias", expectedAlias);

    Action action = tested.create(ACTION_ALIAS, config, null, null);

    // when, then
    verifyActionResult(testContext, action, result -> assertTrue(
        result.result().getFragment().getPayload().containsKey(expectedAlias)));
  }

  @Test
  @DisplayName("Expect JSON in Fragment payload when JSON configured")
  void applyActionWhenJSON(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT);

    Action action = tested.create(ACTION_ALIAS, config, null, null);

    // when, then
    verifyActionResult(testContext, action, result -> assertEquals(EXPECTED_JSON_OBJECT,
        result.result().getFragment().getPayload().getJsonObject(ACTION_ALIAS)));
  }

  @Test
  @DisplayName("Expect JSON array in Fragment payload when JSON array configured")
  void applyActionWhenArray(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_ARRAY);

    Action action = tested.create(ACTION_ALIAS, config, null, null);

    // when, then
    verifyActionResult(testContext, action, result -> assertEquals(EXPECTED_JSON_ARRAY,
        result.result().getFragment().getPayload().getJsonArray(ACTION_ALIAS)));
  }

  @Test
  @DisplayName("Expect all incoming payload entries in result fragment payload.")
  void expectAllIncomingPayloadEntriesInResult(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT);

    Action action = tested.create(ACTION_ALIAS, config, null, null);

    String expectedKey = "input";
    Fragment fragment = someFragment().appendPayload(expectedKey, "any value");

    // when, then
    verifyActionResult(testContext, action, fragment,
        result -> assertTrue(result.result().getFragment().getPayload().containsKey(expectedKey)));
  }

}
