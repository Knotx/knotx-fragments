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

import static io.knotx.fragments.action.library.TestUtils.doActionIdle;
import static io.knotx.fragments.action.library.TestUtils.someFragment;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.ACTION_ALIAS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.Fragment;
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
class InlineBodyActionFactoryTest {

  private static final String INITIAL_BODY = "initial body";
  private static final String BODY_TO_INLINE = "inlined body";

  private InlineBodyActionFactory tested;

  @BeforeEach
  void setUp() {
    tested = new InlineBodyActionFactory();
  }

  @Test
  @DisplayName("Expect IllegalArgumentException when doAction specified.")
  void createActionWithDoAction() {
    // when, then
    assertThrows(IllegalArgumentException.class, () -> tested
        .create(ACTION_ALIAS, new JsonObject(), null, doActionIdle()));
  }

  @Test
  @DisplayName("Expect not empty Fragment body when Action configuration specifies body.")
  void applyAction(VertxTestContext testContext) {
    // given
    Action action = tested.create(ACTION_ALIAS, new JsonObject().put("body",
        BODY_TO_INLINE), null, null);

    // when, then
    verifyActionResult(testContext, action, someFragment(), result -> {
      assertTrue(result.succeeded());
      assertEquals(BODY_TO_INLINE, result.result().getFragment().getBody());
    });
  }

  @Test
  @DisplayName("Expect empty Fragment body when Action configuration does not specify body.")
  void applyActionWithEmptyConfiguration(VertxTestContext testContext) {
    // given
    Action action = tested.create("action", new JsonObject(), null, null);

    Fragment fragment = someFragment().setBody(INITIAL_BODY);

    // when, then
    verifyActionResult(testContext, action, fragment, result -> {
      assertTrue(result.succeeded());
      assertEquals("", result.result().getFragment().getBody());
    });
  }

}
