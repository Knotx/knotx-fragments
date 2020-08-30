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
package io.knotx.fragments.task.factory.generic.node.action.metadata;

import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionEntryTest {

  private static final String ALIAS = "some-alias";
  private static final String FACTORY = "some-factory";
  private static final JsonObject CONFIG = new JsonObject()
      .put("some-config-key", "some-config-value");
  private static final ActionFactoryOptions OPTIONS = new ActionFactoryOptions(FACTORY, CONFIG);

  @Test
  @DisplayName("Expect empty JsonObject when alias is null")
  void nullEntry() {
    ActionEntry tested = new ActionEntry(null, null);

    assertTrue(tested.toMetadata().isEmpty());
  }

  @Test
  @DisplayName("Expect only alias when options is empty")
  void nullOptions() {
    ActionEntry tested = new ActionEntry(ALIAS, null);

    JsonObject expected = new JsonObject()
        .put(ActionEntry.METADATA_ALIAS, ALIAS);

    assertJsonEquals(expected, tested.toMetadata());
  }

  @Test
  @DisplayName("Expect alias and options when provided")
  void aliasAndOptions() {
    ActionEntry tested = new ActionEntry(ALIAS, OPTIONS);

    JsonObject expected = new JsonObject()
        .put(ActionEntry.METADATA_ALIAS, ALIAS)
        .put(ActionEntry.METADATA_ACTION_FACTORY, FACTORY)
        .put(ActionEntry.METADATA_ACTION_CONFIG, CONFIG);

    assertJsonEquals(expected, tested.toMetadata());
  }

}
