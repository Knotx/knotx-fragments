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

import static io.knotx.fragments.action.api.log.ActionLog.INVOCATIONS;
import static io.knotx.fragments.action.api.log.ActionLog.LOGS;
import static io.knotx.fragments.action.library.TestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.TestUtils.someFragment;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.library.InlineBodyActionFactory;
import io.knotx.junit5.KnotxExtension;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
class InlineBodyActionFactoryLoggingTest {

  private static final String BODY_TO_INLINE = "body to inline";
  private static final String INITIAL_BODY = "initial body";

  private static final String ORIGINAL_BODY_KEY = "originalBody";
  private static final String BODY_KEY = "body";

  @Test
  @DisplayName("Logs old and new body when log level is info")
  void applyActionWithInfoLogLevel(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("body", BODY_TO_INLINE)
        .put("logLevel", "info");

    Action action = new InlineBodyActionFactory().create(ACTION_ALIAS, config, null, null);

    // when
    verifyActionResult(testContext, action, someFragment().setBody(INITIAL_BODY), result -> {
      JsonObject logs = result.result().getLog().getJsonObject(LOGS);
      assertEquals(INITIAL_BODY, logs.getString(ORIGINAL_BODY_KEY));
      assertEquals(BODY_TO_INLINE, logs.getString(BODY_KEY));
    });
  }

  @Test
  @DisplayName("Node log is empty when log level is error")
  void applyActionWithErrorLogLevel(VertxTestContext testContext) {
    // given
    JsonObject config = new JsonObject()
        .put("body", BODY_TO_INLINE)
        .put("logLevel", "error");

    Action action = new InlineBodyActionFactory().create(ACTION_ALIAS, config, null, null);

    // when
    verifyActionResult(testContext, action, someFragment().setBody(INITIAL_BODY), result -> {
      JsonObject logs = result.result().getLog();
      assertTrue(logs.getJsonObject(LOGS).isEmpty());
      assertTrue(logs.getJsonArray(INVOCATIONS).isEmpty());
    });
  }

}
