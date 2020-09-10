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
package io.knotx.fragments.action.library.logging;

import static io.knotx.fragments.action.library.TestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.library.InlinePayloadActionFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
class InlinePayloadActionFactoryLoggingTest {

  private static final JsonObject EXPECTED_JSON_OBJECT = new JsonObject()
      .put("data", "default value");
  private static final String KEY_ALIAS = "some key";
  private static final String LOGS_KEY = "logs";
  private static final String KEY_LOG_KEY = "key";
  private static final String VALUE_LOG_KEY = "value";

  @Test
  @DisplayName("Key and payload get logged when INFO log level set")
  void applyActionWhenJSONWithInfoLogLevel(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT)
        .put("logLevel", "info")
        .put("alias", KEY_ALIAS);

    Action action = new InlinePayloadActionFactory().create(ACTION_ALIAS, config, null, null);

    // when
    verifyActionResult(testContext, action, result -> {
      JsonObject log = result.result().getLog().getJsonObject(LOGS_KEY);
      assertEquals(KEY_ALIAS, log.getString(KEY_LOG_KEY));
      assertEquals(EXPECTED_JSON_OBJECT, log.getJsonObject(VALUE_LOG_KEY));
    });
  }

  @Test
  @DisplayName("Empty node log when error log level set")
  void applyActionWhenJSONWithErrorLogLevel(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("payload", EXPECTED_JSON_OBJECT)
        .put("logLevel", "error")
        .put("alias", KEY_ALIAS);

    Action action = new InlinePayloadActionFactory().create(ACTION_ALIAS, config, null, null);

    // when
    verifyActionResult(testContext, action, result -> {
      JsonObject log = result.result().getLog().getJsonObject(LOGS_KEY);
      assertFalse(log.containsKey(KEY_LOG_KEY));
      assertFalse(log.containsKey(VALUE_LOG_KEY));
    });
  }
}
