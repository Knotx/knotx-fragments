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

import static io.knotx.commons.json.JsonObjectUtil.getObject;
import static io.knotx.commons.json.JsonObjectUtil.putValue;
import static io.knotx.fragments.action.library.TestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.TestUtils.doActionIdle;
import static io.knotx.fragments.action.library.TestUtils.someContext;
import static io.knotx.fragments.action.library.TestUtils.someFragmentWithPayload;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.knotx.fragments.api.FragmentContext;
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
class CopyPayloadKeyActionFactoryTest {

  private static final String FROM = "commonParent.fromSelector.nested._result";
  private static final String TO = "commonParent.toSelector.nested.something";

  private static final JsonObject CONFIG = new JsonObject()
      .put("from", FROM)
      .put("to", TO);

  private static final JsonObject SOME_JSON = new JsonObject()
      .put("nested", new JsonObject());
  private static final String SOME_STRING = "test-string";

  private CopyPayloadKeyActionFactory tested;

  @BeforeEach
  void setUp() {
    tested = new CopyPayloadKeyActionFactory();
  }

  @Test
  @DisplayName("Expect exception when doAction specified")
  void doActionSpecified() {
    assertThrows(ActionConfigurationException.class,
        () -> tested.create(ACTION_ALIAS, CONFIG.copy(), null, doActionIdle()));
  }

  @Test
  @DisplayName("Expect exception when config is not valid")
  void configInvalid() {
    assertThrows(ActionConfigurationException.class,
        () -> tested.create(ACTION_ALIAS, new JsonObject(), null, null));
  }

  @Test
  @DisplayName("Expect copied json from existing location to empty one")
  void existingToNewJson(VertxTestContext testContext) {
    Action action = createWithConfig();

    FragmentContext input = fromPresentToEmpty(SOME_JSON.copy());

    verifyPayloadCopied(testContext, action, input, SOME_JSON);
  }

  @Test
  @DisplayName("Expect copied string from existing location to empty one")
  void existingToNewString(VertxTestContext testContext) {
    Action action = createWithConfig();

    FragmentContext input = fromPresentToEmpty(SOME_STRING);

    verifyPayloadCopied(testContext, action, input, SOME_STRING);
  }

  @Test
  @DisplayName("Expect copied long from existing location to empty one")
  void existingToNewLong(VertxTestContext testContext) {
    Action action = createWithConfig();

    FragmentContext input = fromPresentToEmpty(1000L);

    verifyPayloadCopied(testContext, action, input, 1000L);
  }

  @Test
  @DisplayName("Expect copied json from existing location to non existing one")
  void existingToNotExistingJson(VertxTestContext testContext) {
    Action action = createWithConfig();

    FragmentContext input = fromPresentToMissing(SOME_JSON.copy());

    verifyPayloadCopied(testContext, action, input, SOME_JSON);
  }

  @Test
  @DisplayName("Expect payload not copied when value not present")
  void noValue(VertxTestContext testContext) {
    Action action = createWithConfig();

    FragmentContext input = empty();

    verifyPayloadEquals(testContext, action, input, new JsonObject());
  }

  private Action createWithConfig() {
    return tested.create(ACTION_ALIAS, CONFIG.copy(), null, null);
  }

  private void verifyPayloadCopied(VertxTestContext testContext, Action action,
      FragmentContext input, Object value) {
    verifyActionResult(testContext, action, input, result -> {
      JsonObject payload = result.result().getFragment().getPayload();
      assertEquals(value, getObject(FROM, payload));
      assertEquals(value, getObject(TO, payload));
    });
  }

  private void verifyPayloadEquals(VertxTestContext testContext, Action action,
      FragmentContext input, JsonObject expected) {
    verifyActionResult(testContext, action, input, result -> {
      JsonObject payload = result.result().getFragment().getPayload();
      assertEquals(expected, payload);
    });
  }

  private static FragmentContext fromPresentToMissing(Object value) {
    JsonObject payload = new JsonObject();
    putValue(FROM, payload, value);
    return contextWithPayload(payload);
  }

  private static FragmentContext fromPresentToEmpty(Object value) {
    JsonObject payload = new JsonObject();
    putValue(FROM, payload, value);
    putValue(TO, payload, new JsonObject());
    return contextWithPayload(payload);
  }

  private static FragmentContext empty() {
    return contextWithPayload(new JsonObject());
  }

  private static FragmentContext contextWithPayload(JsonObject payload) {
    return someContext(someFragmentWithPayload(payload));
  }

}
