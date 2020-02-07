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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.NodeOptions;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
import io.knotx.fragments.task.factory.node.StubNode;
import io.knotx.fragments.task.factory.GraphNodeOptions;
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

  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();

  @Test
  @DisplayName("Expect composite node with single subgraph.")
  void expectCompositeNodeW(Vertx vertx) {
    // given
    GraphNodeOptions subNodeConfig = new GraphNodeOptions(
        new NodeOptions("someFactory", new JsonObject()),
        NO_TRANSITIONS
    );

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(subNodeConfig),
        NO_TRANSITIONS
    );

    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(eq(subNodeConfig))).thenReturn(new StubNode("A"));

    // when
    Node node = new SubtasksNodeFactory().configure(new JsonObject(), vertx)
        .initNode(graph, Collections.emptyMap(), nodeProvider);

    // then
    assertTrue(node instanceof CompositeNode);
    assertEquals(SubtasksNodeFactory.COMPOSITE_NODE_ID, node.getId());
    assertFalse(node.next(SUCCESS_TRANSITION).isPresent());
    assertFalse(node.next(ERROR_TRANSITION).isPresent());

    CompositeNode compositeRootNode = (CompositeNode) node;
    assertEquals(1, compositeRootNode.getNodes().size());
    Node subNode = compositeRootNode.getNodes().get(0);
    assertEquals("A", subNode.getId());
  }

  @Test
  @DisplayName("Expect only _success and _error transitions.")
  void expectOnlySuccessAndErrorTransitions(Vertx vertx) {
    // given
    GraphNodeOptions anyNodeConfig = new GraphNodeOptions(new JsonObject());

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(anyNodeConfig),
        NO_TRANSITIONS
    );

    NodeProvider nodeProvider = mock(NodeProvider.class);
    when(nodeProvider.initNode(any())).thenReturn(new StubNode("A"));

    Map<String, NodeWithMetadata> transitionsToNodes = new HashMap<>();
    transitionsToNodes.put(SUCCESS_TRANSITION, new StubNode("B"));
    transitionsToNodes.put(ERROR_TRANSITION, new StubNode("C"));
    transitionsToNodes.put("otherTransition", new StubNode("D"));

    // when
    Node node = new SubtasksNodeFactory().configure(new JsonObject(), vertx)
        .initNode(graph, transitionsToNodes, nodeProvider);

    // then
    assertTrue(node.next(SUCCESS_TRANSITION).isPresent());
    assertEquals("B", node.next(SUCCESS_TRANSITION).get().getId());
    assertTrue(node.next(ERROR_TRANSITION).isPresent());
    assertEquals("C", node.next(ERROR_TRANSITION).get().getId());
    assertFalse(node.next("otherTransition").isPresent());
  }

  @Test
  @DisplayName("Expect graph with nested composite nodes")
  void expectNestedCompositeNodesGraph(Vertx vertx) {
    // given
    GraphNodeOptions nestedNodeConfig = new GraphNodeOptions(
        subTasks(
            new GraphNodeOptions(new JsonObject())
        ), NO_TRANSITIONS);

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(
            nestedNodeConfig
        ), NO_TRANSITIONS
    );

    NodeProvider nodeProvider = mock(NodeProvider.class);

    // when
    new SubtasksNodeFactory().configure(new JsonObject(), vertx)
        .initNode(graph, Collections.emptyMap(), nodeProvider);

    // then
    verify(nodeProvider, times(1)).initNode(eq(nestedNodeConfig));

  }

  private List<GraphNodeOptions> subTasks(GraphNodeOptions... nodes) {
    return Arrays.asList(nodes);
  }
}