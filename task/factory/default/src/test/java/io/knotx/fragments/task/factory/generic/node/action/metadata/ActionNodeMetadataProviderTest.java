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

import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.EDGES;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.TRANSITIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Function;

class ActionNodeMetadataProviderTest {

  private static final String NODE_ID = "some-node-id";
  private static final String ACTION_NODE_FACTORY = "custom-action-factory";
  private static final String TEST_ALIAS = "alias-for-test";

  private static final OperationMetadata EMPTY_METADATA = new OperationMetadata(ACTION_NODE_FACTORY, new JsonObject());
  private static final OperationMetadata TEST_METADATA = new OperationMetadata(ACTION_NODE_FACTORY, new JsonObject()
      .put(ActionEntry.METADATA_ALIAS, TEST_ALIAS));

  private static final OperationMetadataProvider EMPTY_PROVIDER = providerWith(alias -> EMPTY_METADATA);
  private static final OperationMetadataProvider TEST_PROVIDER = providerWith(
      alias -> TEST_ALIAS.equals(alias) ? TEST_METADATA : EMPTY_METADATA);

  @Test
  @DisplayName("Expect edges transformed to transitions")
  void edgesToTransitions() {
    ActionNodeMetadataProvider tested = new ActionNodeMetadataProvider(EMPTY_PROVIDER);

    NodeMetadata result = tested.provideFor(NODE_ID, EDGES, "alias-not-existing");

    assertEquals(TRANSITIONS, result.getTransitions());
  }

  @Test
  @DisplayName("Expect not existing alias to be populated as label")
  void notExistingAlias() {
    ActionNodeMetadataProvider tested = new ActionNodeMetadataProvider(EMPTY_PROVIDER);

    NodeMetadata result = tested.provideFor(NODE_ID, EDGES, "alias-not-existing");

    assertEquals("alias-not-existing", result.getLabel());
  }

  @Test
  @DisplayName("Expect single node type")
  void singleNodeType() {
    ActionNodeMetadataProvider tested = new ActionNodeMetadataProvider(EMPTY_PROVIDER);

    NodeMetadata result = tested.provideFor(NODE_ID, EDGES, "alias-not-existing");

    assertEquals(NodeType.SINGLE, result.getType());
  }

  @Test
  @DisplayName("Expect empty nested nodes")
  void emptyNestedNodes() {
    ActionNodeMetadataProvider tested = new ActionNodeMetadataProvider(EMPTY_PROVIDER);

    NodeMetadata result = tested.provideFor(NODE_ID, EDGES, "alias-not-existing");

    assertEquals(Collections.emptyList(), result.getNestedNodes());
  }

  @Test
  @DisplayName("Expect node id populated")
  void nodeIdSet() {
    ActionNodeMetadataProvider tested = new ActionNodeMetadataProvider(EMPTY_PROVIDER);

    NodeMetadata result = tested.provideFor(NODE_ID, EDGES, "alias-not-existing");

    assertEquals(NODE_ID, result.getNodeId());
  }

  @Test
  @DisplayName("Expect operation metadata returned by provider to be populated")
  void operationMetadata() {
    ActionNodeMetadataProvider tested = new ActionNodeMetadataProvider(TEST_PROVIDER);

    NodeMetadata result = tested.provideFor(NODE_ID, EDGES, TEST_ALIAS);

    assertEquals(TEST_METADATA, result.getOperation());
  }

  private static OperationMetadataProvider providerWith(Function<String, OperationMetadata> provision) {
    return new OperationMetadataProvider() {
      @Override
      OperationMetadata provideFor(String alias) {
        return provision.apply(alias);
      }
    };
  }

}
