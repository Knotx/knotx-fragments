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
package io.knotx.fragments.task.factory.config.node.action;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.single.SingleNode;
import io.knotx.fragments.task.factory.generic.GraphNodeOptions;
import io.knotx.fragments.task.factory.generic.NodeProvider;
import io.knotx.fragments.task.factory.generic.node.NodeOptions;
import io.knotx.fragments.task.factory.generic.node.StubNode;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeConfig;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactory;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactoryConfig;
import io.knotx.fragments.task.factory.generic.node.action.ActionNotFoundException;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class ActionNodeFactoryTest {

  // ACTION NODE
  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();
  private static final Map<String, Node> NO_EDGES = Collections.emptyMap();

  // ACTION
  private static final String ACTION_FACTORY_NAME = "test-action";
  private static final JsonObject ACTION_CONFIG = new JsonObject()
      .put("actionConfigKey", "actionConfigValue");

  private static NodeProvider emptyNodeProvider;

  @BeforeEach
  void init() {
    emptyNodeProvider = mock(NodeProvider.class);
    when(emptyNodeProvider.initNode(any(), any())).thenThrow(new IllegalStateException());
  }

  @Test
  @DisplayName("Expect exception when `config.actions` not defined.")
  void expectExceptionWhenActionsNotConfigured(Vertx vertx) {
    // given
    ActionNodeFactoryConfig factoryConfig = new ActionNodeFactoryConfig(Collections.emptyMap());
    NodeOptions nodeOptions = nodeOptions("A");

    // when, then
    assertThrows(
        ActionNotFoundException.class,
        () -> new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
            .initNode(nodeOptions, NO_EDGES, emptyNodeProvider, emptyMetadata()));
  }

  @Test
  @DisplayName("Expect exception when action not found.")
  void expectExceptionWhenActionNotFound(Vertx vertx) {
    // given
    ActionNodeFactoryConfig factoryConfig = factoryConfig("otherAction");
    NodeOptions nodeOptions = nodeOptions("A");

    // when, then
    assertThrows(
        ActionNotFoundException.class,
        () -> new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
            .initNode(nodeOptions, NO_EDGES, emptyNodeProvider, emptyMetadata()));
  }

  @Test
  @DisplayName("Expect single node when action node defined.")
  void expectSingleActionNode(Vertx vertx) {
    // given
    String actionAlias = "A";
    ActionNodeFactoryConfig factoryConfig = factoryConfig(actionAlias);
    NodeOptions nodeOptions = nodeOptions(actionAlias);

    // when
    Node node = new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
        .initNode(nodeOptions, NO_EDGES, emptyNodeProvider, emptyMetadata());

    // then
    assertNotNull(node);
    assertTrue(node instanceof SingleNode);
    assertEquals(NodeType.SINGLE, node.getType());
  }

  @Test
  @Deprecated
  @DisplayName("Expect single node when action node defined.")
  void validateDeprecatedApi(Vertx vertx) {
    // given
    String actionAlias = "A";
    ActionNodeFactoryConfig factoryConfig = factoryConfig(actionAlias);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when
    Node node = new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
        .initNode(graph, NO_EDGES, emptyNodeProvider);

    // then
    assertNotNull(node);
    assertTrue(node instanceof SingleNode);
    assertEquals(NodeType.SINGLE, node.getType());
  }

  @Test
  @DisplayName("Expect action node id is unique.")
  void expectUniqueActionNodeId(Vertx vertx) {
    // given
    String actionAlias = "A";
    ActionNodeFactoryConfig factoryConfig = factoryConfig(actionAlias);
    NodeOptions nodeOptions = nodeOptions(actionAlias);

    ActionNodeFactory tested = new ActionNodeFactory().configure(factoryConfig.toJson(), vertx);

    // when
    Node first = tested
        .initNode(nodeOptions, Collections.emptyMap(), emptyNodeProvider, emptyMetadata());
    Node second = tested.initNode(nodeOptions, NO_EDGES, emptyNodeProvider, emptyMetadata());

    // then
    assertNotEquals(first.getId(), second.getId());
  }

  @Test
  @DisplayName("Expect node contains passed transitions.")
  void expectActionNodesGraphWithTransition(Vertx vertx) {
    // given
    String actionAlias = "A";
    ActionNodeFactoryConfig factoryConfig = factoryConfig(actionAlias);
    NodeOptions nodeOptions = nodeOptions(actionAlias);

    Map<String, Node> edges = Stream.of(
        new AbstractMap.SimpleImmutableEntry<>(SUCCESS_TRANSITION, new StubNode("B")),
        new AbstractMap.SimpleImmutableEntry<>(ERROR_TRANSITION, new StubNode("C")),
        new AbstractMap.SimpleImmutableEntry<>("custom", new StubNode("D")))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // when
    Node node = new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
        .initNode(nodeOptions, edges, emptyNodeProvider, emptyMetadata());

    // then
    assertTrue(node.next(SUCCESS_TRANSITION).isPresent());
    assertTrue(node.next(ERROR_TRANSITION).isPresent());
    assertTrue(node.next("custom").isPresent());
    assertEquals("B", node.next(SUCCESS_TRANSITION).get().getId());
    assertEquals("C", node.next(ERROR_TRANSITION).get().getId());
    assertEquals("D", node.next("custom").get().getId());
  }

  @Test
  @DisplayName("Expect action logic is applied.")
  void expectActionLogicIsApplied(Vertx vertx, VertxTestContext testContext) {
    // given
    String actionAlias = "A";
    ActionNodeFactoryConfig factoryConfig = factoryConfig(actionAlias);
    NodeOptions nodeOptions = nodeOptions(actionAlias);

    // when
    Node node = new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
        .initNode(nodeOptions, NO_EDGES, emptyNodeProvider, emptyMetadata());

    // then
    SingleNode singleNode = (SingleNode) node;
    Single<FragmentResult> result = FragmentOperation.newInstance(singleNode).rxApply(
        new FragmentContext(new Fragment("type", new JsonObject(), "body"), new ClientRequest()));
    result
        .doOnSuccess(response -> testContext.verify(() -> {
          assertEquals(SUCCESS_TRANSITION, response.getTransition());
          testContext.completeNow();
        }))
        .doOnError(testContext::failNow)
        .subscribe();
  }

  @Test
  @DisplayName("Expect metadata to have correct information.")
  void expectMetadata(Vertx vertx) {
    // given
    String actionAlias = "A";
    ActionNodeFactoryConfig factoryConfig = factoryConfig(actionAlias);
    NodeOptions nodeOptions = nodeOptions(actionAlias);

    Map<String, NodeMetadata> nodesMetadata = new HashMap<>();
    Map<String, Node> edges = Stream.of(
        new AbstractMap.SimpleImmutableEntry<>(SUCCESS_TRANSITION, new StubNode("B")),
        new AbstractMap.SimpleImmutableEntry<>(ERROR_TRANSITION, new StubNode("C")),
        new AbstractMap.SimpleImmutableEntry<>("custom", new StubNode("D")))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // when
    Node node = new ActionNodeFactory().configure(factoryConfig.toJson(), vertx)
        .initNode(nodeOptions, edges, emptyNodeProvider, nodesMetadata);

    // then
    Map<String, String> expectedTransitions = Stream.of(new String[][]{
        {SUCCESS_TRANSITION, "B"},
        {ERROR_TRANSITION, "C"},
        {"custom", "D"}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    assertEquals(1, nodesMetadata.size());
    assertTrue(nodesMetadata.values().stream().findFirst().isPresent());

    NodeMetadata metadata = nodesMetadata.values().stream().findFirst().get();
    assertEquals(node.getId(), metadata.getNodeId());
    assertEquals(actionAlias, metadata.getLabel());
    assertEquals(node.getType(), metadata.getType());
    assertEquals(expectedTransitions, metadata.getTransitions());
    assertEquals(Collections.emptyList(), metadata.getNestedNodes());
    assertNotNull(metadata.getOperation());

    assertEquals(ActionNodeFactory.NAME, metadata.getOperation().getFactory());
    JsonObject data = metadata.getOperation().getData();
    assertEquals(actionAlias, data.getString(ActionNodeFactory.METADATA_ALIAS));
    assertEquals(ACTION_FACTORY_NAME, data.getString(ActionNodeFactory.METADATA_ACTION_FACTORY));
    assertEquals(ACTION_CONFIG, data.getJsonObject(ActionNodeFactory.METADATA_ACTION_CONFIG));
  }

  private ActionNodeFactoryConfig factoryConfig(String actionName) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionFactoryOptions(new JsonObject())
            .setFactory(ACTION_FACTORY_NAME)
            .setConfig(ACTION_CONFIG)));
  }

  private NodeOptions nodeOptions(String actionAlias) {
    return new NodeOptions(ActionNodeFactory.NAME, new ActionNodeConfig(actionAlias).toJson());
  }

  private Map<String, NodeMetadata> emptyMetadata() {
    return new HashMap<>();
  }
}
