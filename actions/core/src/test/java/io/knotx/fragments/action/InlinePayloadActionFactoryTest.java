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
package io.knotx.fragments.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class InlinePayloadActionFactoryTest {

  private static final JsonArray EXPECTED_JSON_ARRAY = new JsonArray().add("some value");
  private static final JsonObject EXPECTED_JSON_OBJECT = new JsonObject()
      .put("data", "default value");
  private static final String ACTION_ALIAS = "action";
  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(), "body");

  @Test
  @DisplayName("Expect IllegalArgumentException when payload not configured.")
  void createActionWithoutPayload() {
    // when, then
    assertThrows(IllegalArgumentException.class, () -> new InlinePayloadActionFactory()
        .create(ACTION_ALIAS, new JsonObject(), null, null));
  }

  @Test
  @DisplayName("Expect IllegalArgumentException when doAction specified.")
  void createActionWithDoAction() {
    // when, then
    assertThrows(IllegalArgumentException.class, () -> new InlinePayloadActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("payload", EXPECTED_JSON_OBJECT), null,
            (fragmentContext, resultHandler) -> {
            }));
  }

  @Test
  @DisplayName("Expect payload with action alias key in Fragment payload when alias not configured.")
  void applyActionWithActionAlias(VertxTestContext testContext) throws Throwable {
    // given
    Action action = new InlinePayloadActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("payload", EXPECTED_JSON_OBJECT), null, null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> assertTrue(
              result.result().getFragment().getPayload().containsKey(ACTION_ALIAS)));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect payload with alias key in Fragment payload when alias configured")
  void applyActionWithAlias(VertxTestContext testContext) throws Throwable {
    // given
    String expectedAlias = "newAction";
    Action action = new InlinePayloadActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject().put("alias", expectedAlias).put("payload", EXPECTED_JSON_OBJECT), null,
            null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(
              () -> assertTrue(
                  result.result().getFragment().getPayload().containsKey(expectedAlias)));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect JSON in Fragment payload when JSON configured")
  void applyActionWhenJSON(VertxTestContext testContext) throws Throwable {
    // given
    Action action = new InlinePayloadActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject().put("payload", EXPECTED_JSON_OBJECT), null, null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(
              () -> assertEquals(EXPECTED_JSON_OBJECT,
                  result.result().getFragment().getPayload().getJsonObject(ACTION_ALIAS)));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect JSON array in Fragment payload when JSON array configured")
  void applyActionWhenArray(VertxTestContext testContext) throws Throwable {
    // given
    Action action = new InlinePayloadActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject().put("payload", EXPECTED_JSON_ARRAY), null, null);

    // when
    action.apply(new FragmentContext(FRAGMENT, new ClientRequest()),
        result -> {
          // then
          testContext.verify(
              () -> assertEquals(EXPECTED_JSON_ARRAY,
                  result.result().getFragment().getPayload().getJsonArray(ACTION_ALIAS)));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect all incoming payload entries in result fragment payload.")
  void expectAllIncomingPayloadEntriesInResult(VertxTestContext testContext) throws Throwable {
    // given
    String expectedKey = "input";

    Fragment fragment = new Fragment("type", new JsonObject(), "body");
    fragment.appendPayload(expectedKey, "any value");
    Action action = new InlinePayloadActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject().put("payload", EXPECTED_JSON_OBJECT), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(
              () -> assertTrue(result.result().getFragment().getPayload().containsKey(expectedKey)));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}