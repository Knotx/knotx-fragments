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

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class InlineBodyActionFactoryTest {

  private static final String ACTION_ALIAS = "action";
  private static final String EXPECTED_VALUE = "expected value";
  private static final String INITIAL_BODY = "initial body";

  @Test
  @DisplayName("Expect IllegalArgumentException when doAction specified.")
  void createActionWithDoAction() {
    // when, then
    assertThrows(IllegalArgumentException.class, () -> new InlineBodyActionFactory()
        .create(ACTION_ALIAS, new JsonObject(), null,
            (fragmentContext, resultHandler) -> {
            }));
  }

  @Test
  @DisplayName("Expect not empty Fragment body when Action configuration specifies body.")
  void applyAction(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create(ACTION_ALIAS, new JsonObject().put("body",
        EXPECTED_VALUE), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            Assertions.assertTrue(result.succeeded());
            Assertions.assertEquals(EXPECTED_VALUE, result.result().getFragment().getBody());
          });
          testContext.completeNow();
        });

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect empty Fragment body when Action configuration does not specify body.")
  void applyActionWithEmptyConfiguration(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create("action", new JsonObject(), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            Assertions.assertTrue(result.succeeded());
            Assertions.assertEquals("", result.result().getFragment().getBody());
          });
          testContext.completeNow();
        });

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}