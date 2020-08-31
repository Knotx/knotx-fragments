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
package io.knotx.fragments.task.factory.generic.node.action;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.EDGES;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.TRANSITIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.task.api.single.SingleNode;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.knotx.fragments.task.factory.generic.GraphNodeOptions;
import io.knotx.fragments.task.factory.generic.NodeProvider;
import io.knotx.fragments.task.factory.generic.node.NodeOptions;
import io.knotx.fragments.task.factory.generic.node.StubNode;
import io.knotx.fragments.task.factory.generic.node.action.metadata.ActionEntry;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(VertxExtension.class)
class ActionNodeFactoryTest {

  // ACTION NODE
  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();
  private static final Map<String, Node> NO_EDGES = Collections.emptyMap();

  // ACTION
  private static final String ALIAS = "A";
  public static final String DO_ACTION_ALIAS = "doActionAlias";
  private static final String ACTION_FACTORY_NAME = "test-action";
  private static final JsonObject ACTION_CONFIG = new JsonObject()
      .put("actionConfigKey", "actionConfigValue");

  private static final FragmentContext FRAGMENT_CONTEXT = new FragmentContext(
      new Fragment("type", new JsonObject(), "body"), new ClientRequest());

  private static final NodeProvider emptyNodeProvider = nodeOptions -> {
    throw new IllegalStateException();
  };

  @Test
  @DisplayName("Expect exception when `config.actions` not defined.")
  void expectExceptionWhenActionsNotConfigured(Vertx vertx) {
    ActionNodeFactory tested = withNoActionsConfigured(vertx);

    assertThrows(ActionNotFoundException.class,
        () -> tested.initNode(nodeSelecting(ALIAS), NO_EDGES, emptyNodeProvider, emptyMetadata()));
  }

  @Test
  @DisplayName("Expect exception when action not found.")
  void expectExceptionWhenActionNotFound(Vertx vertx) {
    ActionNodeFactory tested = withSingleActionConfigured("otherAction", vertx);

    assertThrows(ActionNotFoundException.class,
        () -> tested.initNode(nodeSelecting(ALIAS), NO_EDGES, emptyNodeProvider, emptyMetadata()));
  }

  @Test
  @DisplayName("Expect single node when action node defined.")
  void expectSingleActionNode(Vertx vertx) {
    ActionNodeFactory tested = withSingleActionConfigured(ALIAS, vertx);

    Node node = tested.initNode(nodeSelecting(ALIAS), NO_EDGES, emptyNodeProvider, emptyMetadata());

    assertNotNull(node);
    assertTrue(node instanceof SingleNode);
    assertEquals(NodeType.SINGLE, node.getType());
  }

  @Test
  @Deprecated
  @DisplayName("Expect single node when action node defined.")
  void validateDeprecatedApi(Vertx vertx) {
    ActionNodeFactory tested = withSingleActionConfigured(ALIAS, vertx);

    GraphNodeOptions graph = new GraphNodeOptions(ALIAS, NO_TRANSITIONS);

    Node node = tested.initNode(graph, NO_EDGES, emptyNodeProvider);

    assertNotNull(node);
    assertTrue(node instanceof SingleNode);
    assertEquals(NodeType.SINGLE, node.getType());
  }

  @Test
  @DisplayName("Expect action node id is unique.")
  void expectUniqueActionNodeId(Vertx vertx) {
    ActionNodeFactory tested = withSingleActionConfigured(ALIAS, vertx);

    Node first = tested
        .initNode(nodeSelecting(ALIAS), NO_EDGES, emptyNodeProvider, emptyMetadata());
    Node second = tested
        .initNode(nodeSelecting(ALIAS), NO_EDGES, emptyNodeProvider, emptyMetadata());

    assertNotEquals(first.getId(), second.getId());
  }

  @Test
  @DisplayName("Expect node contains passed transitions.")
  void expectActionNodesGraphWithTransition(Vertx vertx) {
    ActionNodeFactory tested = withSingleActionConfigured(ALIAS, vertx);

    Node node = tested.initNode(nodeSelecting(ALIAS), EDGES, emptyNodeProvider, emptyMetadata());

    assertTrue(node.next(SUCCESS_TRANSITION).isPresent());
    assertTrue(node.next(ERROR_TRANSITION).isPresent());
    assertTrue(node.next("_fallback").isPresent());
    assertEquals("next-success", node.next(SUCCESS_TRANSITION).get().getId());
    assertEquals("next-error", node.next(ERROR_TRANSITION).get().getId());
    assertEquals("next-fallback", node.next("_fallback").get().getId());
  }

  @Test
  @DisplayName("Expect action logic is applied.")
  void expectActionLogicIsApplied(Vertx vertx, VertxTestContext testContext) {
    ActionNodeFactory tested = withSingleActionConfigured(ALIAS, vertx);

    Node node = tested.initNode(nodeSelecting(ALIAS), NO_EDGES, emptyNodeProvider, emptyMetadata());

    SingleNode singleNode = (SingleNode) node;
    FragmentOperation.newInstance(singleNode)
        .rxApply(FRAGMENT_CONTEXT)
        .subscribe(response -> testContext.verify(() -> {
              assertEquals(SUCCESS_TRANSITION, response.getTransition());
              testContext.completeNow();
            }),
            testContext::failNow);
  }

  @Test
  @DisplayName("Expect metadata to have correct information.")
  void expectMetadata(Vertx vertx) {
    ActionNodeFactory tested = withSingleActionConfigured(ALIAS, vertx);

    Map<String, NodeMetadata> nodesMetadata = new HashMap<>();

    Node node = tested.initNode(nodeSelecting(ALIAS), EDGES, emptyNodeProvider, nodesMetadata);

    JsonObject expectedConfig = new JsonObject()
        .put(ActionEntry.METADATA_ALIAS, ALIAS)
        .put(ActionEntry.METADATA_ACTION_FACTORY, ACTION_FACTORY_NAME)
        .put(ActionEntry.METADATA_ACTION_CONFIG, ACTION_CONFIG);

    OperationMetadata expectedOperation = new OperationMetadata(ActionNodeFactory.NAME,
        expectedConfig);
    NodeMetadata expected = NodeMetadata
        .single(node.getId(), ALIAS, TRANSITIONS, expectedOperation);

    assertEquals(1, nodesMetadata.size());
    assertEquals(expected, nodesMetadata.get(node.getId()));
  }

  @Test
  @DisplayName("Expect metadata to have doAction node details.")
  void expectMetadataWithDoAction(Vertx vertx) {
    ActionNodeFactory tested = withDoActionConfigured(ALIAS, DO_ACTION_ALIAS, vertx);

    Map<String, NodeMetadata> nodesMetadata = new HashMap<>();

    Node node = tested
        .initNode(nodeSelecting(ALIAS), EDGES, nodeOptions -> new StubNode("doActionAlias"),
            nodesMetadata);

    JsonObject expectedConfig = new JsonObject()
        .put(ActionEntry.METADATA_ALIAS, ALIAS)
        .put(ActionEntry.METADATA_ACTION_FACTORY, ACTION_FACTORY_NAME)
        .put(ActionEntry.METADATA_ACTION_CONFIG, ACTION_CONFIG)
        .put(ActionEntry.METADATA_DO_ACTION,
            new JsonObject().put(ActionEntry.METADATA_ALIAS, "doActionAlias"));

    OperationMetadata expectedOperation = new OperationMetadata(ActionNodeFactory.NAME,
        expectedConfig);
    NodeMetadata expected = NodeMetadata
        .single(node.getId(), ALIAS, TRANSITIONS, expectedOperation);

    assertEquals(1, nodesMetadata.size());
    assertEquals(expected, nodesMetadata.get(node.getId()));
  }

  private ActionNodeFactory withNoActionsConfigured(Vertx vertx) {
    return new ActionNodeFactory()
        .configure(new ActionNodeFactoryConfig(Collections.emptyMap()).toJson(), vertx);
  }

  private ActionNodeFactory withSingleActionConfigured(String alias, Vertx vertx) {
    return new ActionNodeFactory().configure(factoryConfig(alias).toJson(), vertx);
  }

  private ActionNodeFactory withDoActionConfigured(String alias, String doActionAlias,
      Vertx vertx) {
    return new ActionNodeFactory().configure(factoryConfig(alias, doActionAlias).toJson(), vertx);
  }

  private ActionNodeFactoryConfig factoryConfig(String actionName) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionFactoryOptions(new JsonObject())
            .setFactory(ACTION_FACTORY_NAME)
            .setConfig(ACTION_CONFIG)));
  }

  private ActionNodeFactoryConfig factoryConfig(String actionName, String doActionName) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionFactoryOptions(new JsonObject())
            .setFactory(ACTION_FACTORY_NAME)
            .setConfig(ACTION_CONFIG)
            .setDoAction(doActionName)));
  }

  private NodeOptions nodeSelecting(String actionAlias) {
    return new NodeOptions(ActionNodeFactory.NAME, new ActionNodeConfig(actionAlias).toJson());
  }

  private Map<String, NodeMetadata> emptyMetadata() {
    return new HashMap<>();
  }
}
