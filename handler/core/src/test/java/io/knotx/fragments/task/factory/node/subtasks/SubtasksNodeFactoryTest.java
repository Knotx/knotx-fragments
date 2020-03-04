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
package io.knotx.fragments.task.factory.node.subtasks;

import static io.knotx.fragments.engine.api.node.single.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.task.factory.node.subtasks.SubtasksNodeFactory.NAME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.composite.CompositeNode;
import io.knotx.fragments.task.exception.NodeFactoryNotFoundException;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.NodeOptions;
import io.knotx.fragments.task.factory.node.StubNode;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class SubtasksNodeFactoryTest {

  private static final String FACTORY_NAME = "factoryName";
  private static final JsonObject FACTORY_CONFIG = new JsonObject();

  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();
  private static final Map<String, Node> NO_EDGES = Collections.emptyMap();


  @Test
  @DisplayName("Node is composite.")
  void expectComposite(Vertx vertx) {
    // given
    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(any(), any())).thenThrow(new IllegalStateException());

    NodeOptions nodeOptions = nodeOptions();

    // when
    Node node = new SubtasksNodeFactory().configure(FACTORY_CONFIG, vertx)
        .initNode(nodeOptions, NO_EDGES, nodeProvider, emptyMetadata());

    // then
    assertEquals(NodeType.COMPOSITE, node.getType());
    assertTrue(node instanceof CompositeNode);
  }

  @Test
  @DisplayName("Expect composite node with no nodes.")
  void expectCompositeWithNoNodes(Vertx vertx) {
    // given
    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(any(), any())).thenThrow(new IllegalStateException());

    NodeOptions nodeOptions = nodeOptions();

    // when
    Node node = new SubtasksNodeFactory().configure(FACTORY_CONFIG, vertx)
        .initNode(nodeOptions, NO_EDGES, nodeProvider, emptyMetadata());

    // then
    assertTrue(((CompositeNode) node).getNodes().isEmpty());
  }

  @Test
  @DisplayName("Expect exception when subtask can not be initialized.")
  void expectExceptionWhenSubtaskNotInitialized(Vertx vertx) {
    // given
    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(any(), any()))
        .thenThrow(new NodeFactoryNotFoundException(FACTORY_NAME));

    GraphNodeOptions subNodeConfig = new GraphNodeOptions(
        new NodeOptions(FACTORY_NAME, new JsonObject()),
        NO_TRANSITIONS
    );
    NodeOptions nodeOptions = new NodeOptions(NAME,
        new SubtasksNodeConfig(singletonList(subNodeConfig)).toJson());

    assertThrows(NodeFactoryNotFoundException.class,
        () -> new SubtasksNodeFactory().configure(FACTORY_CONFIG, vertx)
            .initNode(nodeOptions, NO_EDGES, nodeProvider, emptyMetadata()));
  }

  @Test
  @DisplayName("Expect composite with nodes.")
  void expectCompositeWithNodes(Vertx vertx) {
    // given
    GraphNodeOptions subNodeConfig = new GraphNodeOptions(
        new NodeOptions(FACTORY_NAME, new JsonObject()),
        NO_TRANSITIONS
    );
    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(eq(subNodeConfig), any())).thenReturn(new StubNode("A"));

    NodeOptions nodeOptions = new NodeOptions(NAME,
        new SubtasksNodeConfig(singletonList(subNodeConfig)).toJson());

    // when
    Node node = new SubtasksNodeFactory().configure(FACTORY_CONFIG, vertx)
        .initNode(nodeOptions, Collections.emptyMap(), nodeProvider, new HashMap<>());

    // then
    CompositeNode compositeRootNode = (CompositeNode) node;
    assertEquals("A", compositeRootNode.getNodes().get(0).getId());
  }

  @Test
  @DisplayName("Expect only _success and _error transitions.")
  void expectOnlySuccessAndErrorTransitions(Vertx vertx) {
    // given
    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(any(), any())).thenThrow(new IllegalStateException());

    Map<String, Node> transitionsToNodes = new HashMap<>();
    transitionsToNodes.put(SUCCESS_TRANSITION, new StubNode("B"));
    transitionsToNodes.put(ERROR_TRANSITION, new StubNode("C"));
    transitionsToNodes.put("otherTransition", new StubNode("D"));

    NodeOptions nodeOptions = nodeOptions();

    // when
    Node node = new SubtasksNodeFactory().configure(FACTORY_CONFIG, vertx)
        .initNode(nodeOptions, transitionsToNodes, nodeProvider, emptyMetadata());

    // then
    assertTrue(node.next(SUCCESS_TRANSITION).isPresent());
    assertEquals("B", node.next(SUCCESS_TRANSITION).get().getId());
    assertTrue(node.next(ERROR_TRANSITION).isPresent());
    assertEquals("C", node.next(ERROR_TRANSITION).get().getId());
    assertFalse(node.next("otherTransition").isPresent());
  }

  // TODO verify node metadata

  private NodeOptions nodeOptions() {
    return new NodeOptions(NAME,
        new SubtasksNodeConfig(Collections.emptyList()).toJson());
  }

  private List<GraphNodeOptions> subTasks(GraphNodeOptions... nodes) {
    return Arrays.asList(nodes);
  }

  private Map<String, NodeMetadata> emptyMetadata() {
    return new HashMap<>();
  }
}
