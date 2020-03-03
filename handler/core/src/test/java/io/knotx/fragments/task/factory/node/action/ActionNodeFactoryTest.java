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
package io.knotx.fragments.task.factory.node.action;

import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.OperationMetadata;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.single.SingleNode;
import io.knotx.fragments.task.factory.ActionFactoryOptions;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.node.StubNode;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import jdk.dynalink.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.knotx.fragments.engine.api.node.single.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ActionNodeFactoryTest {

  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();

  @Test
  @DisplayName("Expect exception when `config.actions` not defined.")
  void expectExceptionWhenActionsNotConfigured(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = new JsonObject();
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when, then
    Assertions.assertThrows(
        ActionNotFoundException.class, () -> new ActionNodeFactory().configure(config, vertx)
            .initNode(graph, Collections.emptyMap(), null, Collections.emptyMap()));
  }

  @Test
  @DisplayName("Expect exception when action not found.")
  void expectExceptionWhenActionNotFound(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig("otherAction");
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when, then
    Assertions.assertThrows(
        ActionNotFoundException.class, () -> new ActionNodeFactory().configure(config, vertx)
            .initNode(graph, Collections.emptyMap(), null, Collections.emptyMap()));
  }

  @Test
  @DisplayName("Expect single node when action node defined.")
  void expectSingleActionNode(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig(actionAlias);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when
    Node node = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, Collections.emptyMap(), null, new HashMap<>());

    // then
    assertTrue(node instanceof SingleNode);
    assertEquals(NodeType.SINGLE, node.getType());
  }

  @Test
  @DisplayName("Deprecated: Expect single node when action node defined.")
  void validateDeprecatedApi(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig(actionAlias);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when
    Node node = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, Collections.emptyMap(), null, new HashMap<>());

    // then
    assertTrue(node instanceof SingleNode);
    assertEquals(NodeType.SINGLE, node.getType());
  }

  @Test
  @DisplayName("Expect action node id is unique.")
  void expectUniqueActionNodeId(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig(actionAlias);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when
    Node first = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, Collections.emptyMap(), null, new HashMap<>());
    // when
    Node second = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, Collections.emptyMap(), null, new HashMap<>());

    // then
    assertNotEquals(first.getId(), second.getId());
  }

  // TODO verify that action function is execution works

  @Test
  @DisplayName("Expect node contains passed transitions.")
  void expectActionNodesGraphWithTransition(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig(actionAlias);
    // this invalid configuration is expected
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when
    Map<String, Node> transitionToNode = Stream.of(
        new AbstractMap.SimpleImmutableEntry<>(SUCCESS_TRANSITION, new StubNode("B")),
        new AbstractMap.SimpleImmutableEntry<>(ERROR_TRANSITION, new StubNode("C")),
        new AbstractMap.SimpleImmutableEntry<>("custom", new StubNode("D")))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    Node node = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, transitionToNode, null, new HashMap<>());

    // then
    assertTrue(node.next(SUCCESS_TRANSITION).isPresent());
    assertTrue(node.next(ERROR_TRANSITION).isPresent());
    assertTrue(node.next("custom").isPresent());
    assertEquals("B", node.next(SUCCESS_TRANSITION).get().getId());
    assertEquals("C", node.next(ERROR_TRANSITION).get().getId());
    assertEquals("D", node.next("custom").get().getId());
  }

  @Test
  @DisplayName("Expect metadata to have correct information")
  void expectMetadataToHaveCorrectTransitions(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig(actionAlias);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, Collections.emptyMap());
    Map<String, NodeMetadata> nodesMetadata = new HashMap<>();

    // when
    Node node = new ActionNodeFactory().configure(config, vertx).initNode(graph, Collections.emptyMap(), null, nodesMetadata);

    // then
    assertEquals(1, nodesMetadata.entrySet().size());

    NodeMetadata rootMetadata = nodesMetadata.values().stream().findFirst().get();

    assertEquals(node.getId(), rootMetadata.getNodeId());
    assertEquals(actionAlias, rootMetadata.getLabel());
    assertEquals(node.getType(), rootMetadata.getType());
    assertEquals(Collections.emptyMap(), rootMetadata.getTransitions());
    assertEquals(Collections.emptyList(), rootMetadata.getNestedNodes());
    assertNotNull(rootMetadata.getOperation());

    JsonObject operationMetadata = rootMetadata.getOperation().getData();

    assertEquals(actionAlias, operationMetadata.getString("alias"));
    assertEquals("test-action", operationMetadata.getString("actionFactory"));
  }

  private JsonObject createNodeConfig(String actionName) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionFactoryOptions(new JsonObject())
            .setFactory("test-action")))
        .toJson();
  }
}
