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
package io.knotx.fragments.handler.action;

import static io.knotx.fragments.handler.api.actionlog.ActionLogMode.ERROR;
import static io.knotx.fragments.handler.api.actionlog.ActionLogMode.INFO;
import static io.knotx.fragments.handler.api.actionlog.ActionLogger.getLogs;
import static io.knotx.fragments.handler.api.actionlog.ActionLogger.getStringLogEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionConfig;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class PayloadToBodyActionFactoryTest {

  private static final String ACTION_ALIAS = "action";
  private static final String PAYLOAD_KEY = "key";

  private static final JsonObject USER = new JsonObject().put("name", "kovalsky");
  private static final JsonObject NESTED_PAYLOAD = new JsonObject().put("user", USER);
  private static final JsonObject PAYLOAD = new JsonObject().put("key", NESTED_PAYLOAD);
  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(),
      "").mergeInPayload(PAYLOAD);


  @Test
  @DisplayName("Expect IllegalArgumentException when doAction specified.")
  void createActionWithDoAction() {
    // when, then
    assertThrows(IllegalArgumentException.class, () -> new PayloadToBodyActionFactory()
        .create(new ActionConfig(ACTION_ALIAS,
                (fragmentContext, resultHandler) -> {
                }, new JsonObject().put("key", PAYLOAD_KEY), ERROR),
            null));
  }

  @Test
  @DisplayName("Expect body with nested payload under paylod key.")
  void bodyWithNestedPayload(VertxTestContext testContext) throws Throwable {
    // given
    Action action = new PayloadToBodyActionFactory()
        .create(
            new ActionConfig(ACTION_ALIAS, new JsonObject().put(PAYLOAD_KEY, "key"), ERROR),
            null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            String body = result.result().getFragment().getBody();
            assertTrue(result.succeeded());
            assertTrue(getLogs(result.result().getActionLog()).isEmpty());
            assertEquals(new JsonObject(body), NESTED_PAYLOAD);
          });

          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect body with nested payload under paylod key in action log entry.")
  void bodyLoggedWithNestedPayload(VertxTestContext testContext) throws Throwable {
    // given
    Action action = new PayloadToBodyActionFactory()
        .create(new ActionConfig(ACTION_ALIAS,
            new JsonObject().put(PAYLOAD_KEY, "key"), INFO), null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            String body = result.result().getFragment().getBody();
            assertEquals(body, getStringLogEntry("body", result.result().getActionLog()));
            assertEquals(new JsonObject(body), NESTED_PAYLOAD);
          });

          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect body with user payload under paylod key.user.")
  void applyActionWithNestedKey(VertxTestContext testContext) throws Throwable {
    // given
    Action action = new PayloadToBodyActionFactory()
        .create(
            new ActionConfig(ACTION_ALIAS,  new JsonObject().put(PAYLOAD_KEY, "key.user"),
                ERROR), null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            String body = result.result().getFragment().getBody();
            assertTrue(result.succeeded());
            assertEquals(new JsonObject(body), USER);
          });
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}
