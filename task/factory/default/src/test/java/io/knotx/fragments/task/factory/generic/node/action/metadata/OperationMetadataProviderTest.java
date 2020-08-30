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

import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_A;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_B;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_C;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_MISCONFIGURED;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_NOT_EXISTING;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactory;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class OperationMetadataProviderTest {

  private static final Map<String, List<ActionEntry>> TEST_CONFIG = ImmutableMap.of(
      "alias-A", ImmutableList.of(ALIAS_A, ALIAS_B, ALIAS_C),
      "alias-B", ImmutableList.of(ALIAS_B, ALIAS_C),
      "alias-C", ImmutableList.of(ALIAS_C),
      "alias-misconfigured", ImmutableList.of(ALIAS_MISCONFIGURED, ALIAS_NOT_EXISTING)
  );

  private static final ActionLookup LOOKUP = new ActionLookup() {
    @Override
    Iterable<ActionEntry> doActionsFrom(String alias) {
      if (alias != null) {
        return Optional.of(alias)
            .map(TEST_CONFIG::get)
            .orElseGet(() -> ImmutableList.of(new ActionEntry(alias, null)));
      }
      return ImmutableList.of();
    }
  };

  @Test
  @DisplayName("Expect node factory set")
  void customNodeFactory() {
    OperationMetadataProvider tested = new OperationMetadataProvider("custom", LOOKUP);

    OperationMetadata result = tested.provideFor("alias-not-existing");

    assertEquals(result.getFactory(), "custom");
  }

  @Test
  @DisplayName("Expect null argument to yield empty data field")
  void nullAlias() {
    OperationMetadataProvider tested = new OperationMetadataProvider(ActionNodeFactory.NAME, LOOKUP);

    OperationMetadata result = tested.provideFor(null);

    assertTrue(result.getData().isEmpty());
  }

  @Test
  @DisplayName("Expect non-existing alias to yield single entry in data field")
  void nonExistingAlias() {
    OperationMetadataProvider tested = new OperationMetadataProvider(ActionNodeFactory.NAME, LOOKUP);

    OperationMetadata result = tested.provideFor("alias-not-existing");

    JsonObject expected = ALIAS_NOT_EXISTING.toMetadata();

    assertJsonEquals(expected, result.getData());
  }

  @Test
  @DisplayName("Expect two-level information when two elements returned and last is non-existing alias")
  void twoElementsLastInvalid() {
    OperationMetadataProvider tested = new OperationMetadataProvider(ActionNodeFactory.NAME, LOOKUP);

    OperationMetadata result = tested.provideFor("alias-misconfigured");

    JsonObject expected = ALIAS_MISCONFIGURED.toMetadata()
        .put(ActionEntry.METADATA_DO_ACTION, ALIAS_NOT_EXISTING.toMetadata());

    assertJsonEquals(expected, result.getData());
  }

  @Test
  @DisplayName("Expect two-level information when two valid elements returned")
  void twoElementsReturned() {
    OperationMetadataProvider tested = new OperationMetadataProvider(ActionNodeFactory.NAME, LOOKUP);

    OperationMetadata result = tested.provideFor("alias-B");

    JsonObject expected = ALIAS_B.toMetadata()
        .put(ActionEntry.METADATA_DO_ACTION, ALIAS_C.toMetadata());

    assertJsonEquals(expected, result.getData());
  }

  @Test
  @DisplayName("Expect three-level information when three valid elements returned")
  void threeElementsReturned() {
    OperationMetadataProvider tested = new OperationMetadataProvider(ActionNodeFactory.NAME, LOOKUP);

    OperationMetadata result = tested.provideFor("alias-A");

    JsonObject expected = ALIAS_A.toMetadata()
        .put(ActionEntry.METADATA_DO_ACTION, ALIAS_B.toMetadata()
            .put(ActionEntry.METADATA_DO_ACTION, ALIAS_C.toMetadata()));

    assertJsonEquals(expected, result.getData());
  }


}
