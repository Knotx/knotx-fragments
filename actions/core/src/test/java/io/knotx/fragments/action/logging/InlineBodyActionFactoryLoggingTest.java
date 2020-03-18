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
package io.knotx.fragments.action.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.action.InlineBodyActionFactory;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class InlineBodyActionFactoryLoggingTest {

  private static final String LOGS_KEY = "logs";
  private static final String DO_ACTION_LOGS_KEY = "doActionLogs";

  private static final String ACTION_ALIAS = "action";
  private static final String BODY_TO_INLINE = "body to inline";
  private static final String INITIAL_BODY = "initial body";

  private static final String ORIGINAL_BODY_KEY = "originalBody";
  private static final String BODY_KEY = "body";

  @Test
  @DisplayName("Logs old and new body when log level is info")
  void applyActionWithInfoLogLevel(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create(ACTION_ALIAS, new JsonObject().put("body",
        BODY_TO_INLINE).put("logLevel", "info"), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject logs = result.result().getLog().getJsonObject(LOGS_KEY);
            assertEquals(INITIAL_BODY, logs.getString(ORIGINAL_BODY_KEY));
            assertEquals(BODY_TO_INLINE, logs.getString(BODY_KEY));
          });
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Node log is empty when log level is error")
  void applyActionWithErrorLogLevel(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create(ACTION_ALIAS, new JsonObject().put("body",
        BODY_TO_INLINE).put("logLevel", "error"), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject logs = result.result().getLog();
            assertTrue(logs.getJsonObject(LOGS_KEY).isEmpty());
            assertTrue(logs.getJsonArray(DO_ACTION_LOGS_KEY).isEmpty());
          });
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
