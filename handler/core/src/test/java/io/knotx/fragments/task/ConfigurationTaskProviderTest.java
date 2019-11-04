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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.task;

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.options.FragmentsHandlerOptions;
import io.knotx.fragments.task.exception.GraphConfigurationException;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigurationTaskProviderTest {

  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();
  private static final String TASK_NAME = "task";
  private static final String COMPOSITE_NODE_ID = "composite";
  private static final FragmentEventContext SAMPLE_FRAGMENT_EVENT =
      new FragmentEventContext(new FragmentEvent(new Fragment("type",
          new JsonObject().put(FragmentsHandlerOptions.DEFAULT_TASK_KEY, TASK_NAME), "body")),
          new ClientRequest());

  private static final String MY_TASK_KEY = "myTaskKey";

  private static final FragmentEventContext SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY =
      new FragmentEventContext(new FragmentEvent(new Fragment("type",
          new JsonObject().put(MY_TASK_KEY, TASK_NAME), "body")),
          new ClientRequest());

  @Mock
  private ActionProvider actionProvider;

  @Mock
  private Action actionMock;

  @Test
  @DisplayName("Expect graph when custom task key is defined.")
  void expectGraphWhenCustomTaskKey() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions("simpleAction", NO_TRANSITIONS);

    // when
    Task task = new ConfigurationTaskProvider(actionProvider)
        .newInstance(new Configuration(TASK_NAME, graph), SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY);

    // then
    assertEquals(TASK_NAME, task.getName());
  }

  @Test
  @DisplayName("Expect exception when action not defined.")
  void expectExceptionWhenActionNotConfigured() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.empty());

    GraphNodeOptions graph = new GraphNodeOptions("simpleAction", NO_TRANSITIONS);

    // when, then
    Assertions.assertThrows(GraphConfigurationException.class,
        () -> getTask(graph));
  }

  @Test
  @DisplayName("Expect graph with single action node without transitions.")
  void expectSingleActionNodeGraph() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions("simpleAction", NO_TRANSITIONS);

    // when
    Task task = getTask(graph);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof SingleNode);
    assertEquals("simpleAction", rootNode.getId());
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
  }

  @Test
  @DisplayName("Expect graph of two action nodes with transition between.")
  void expectActionNodesGraphWithTransition() {
    // given
    when(actionProvider.get("actionA")).thenReturn(Optional.of(actionMock));
    when(actionProvider.get("actionB")).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions("actionA", Collections
        .singletonMap("customTransition",
            new GraphNodeOptions("actionB", NO_TRANSITIONS)));

    // when
    Task task = getTask(graph);

    // then
    assertEquals(TASK_NAME, task.getName());

    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof SingleNode);
    assertEquals("actionA", rootNode.getId());
    Optional<Node> customNode = rootNode.next("customTransition");
    assertTrue(customNode.isPresent());
    assertTrue(customNode.get() instanceof SingleNode);
    SingleNode customSingleNode = (SingleNode) customNode.get();
    assertEquals("actionB", customSingleNode.getId());
  }

  @Test
  @DisplayName("Expect graph with single composite node without transitions.")
  void expectSingleCompositeNodeGraphWithNoEdges() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("simpleAction", NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    Task task = getTask(graph);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals("composite", rootNode.getId());
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
    assertFalse(rootNode.next(ERROR_TRANSITION).isPresent());

    CompositeNode compositeRootNode = (CompositeNode) rootNode;
    assertEquals(1, compositeRootNode.getNodes().size());
    Node node = compositeRootNode.getNodes().get(0);
    assertTrue(node instanceof SingleNode);
    assertEquals("simpleAction", node.getId());
  }


  @Test
  @DisplayName("Expect graph with composite node and success transition to action node.")
  void expectCompositeNodeWithSingleNodeOnSuccessGraph() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));
    when(actionProvider.get(eq("lastAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("simpleAction", NO_TRANSITIONS)),
        Collections
            .singletonMap(SUCCESS_TRANSITION, new GraphNodeOptions("lastAction", NO_TRANSITIONS))
    );

    // when
    Task task = getTask(graph);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals("composite", rootNode.getId());
    Optional<Node> onSuccess = rootNode.next(SUCCESS_TRANSITION);
    assertTrue(onSuccess.isPresent());
    Node onSuccessNode = onSuccess.get();
    assertTrue(onSuccessNode instanceof SingleNode);
    assertEquals("lastAction", onSuccessNode.getId());
  }

  @Test
  @DisplayName("Expect graph with composite node and error transition to action node.")
  void expectCompositeNodeWithSingleNodeOnErrorGraph() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));
    when(actionProvider.get(eq("fallbackAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("simpleAction", NO_TRANSITIONS)),
        Collections.singletonMap(ERROR_TRANSITION,
            new GraphNodeOptions("fallbackAction", NO_TRANSITIONS))
    );

    // when
    Task task = getTask(graph);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals("composite", rootNode.getId());
    Optional<Node> onError = rootNode.next(ERROR_TRANSITION);
    assertTrue(onError.isPresent());
    Node onErrorNode = onError.get();
    assertTrue(onErrorNode instanceof SingleNode);
    assertEquals("fallbackAction", onErrorNode.getId());
  }

  @Test
  void expectCompositeNodeAcceptsOnlySuccessAndErrorTransitions() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));
    when(actionProvider.get(eq("lastAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("simpleAction", NO_TRANSITIONS)),
        Collections
            .singletonMap("customTransition", new GraphNodeOptions("lastAction", NO_TRANSITIONS))
    );

    // when
    Task task = getTask(graph);

    // then
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
    assertFalse(rootNode.next(ERROR_TRANSITION).isPresent());
    assertFalse(rootNode.next("customTransition").isPresent());
  }

  @Test
  @DisplayName("Expect graph with nested composite nodes")
  void expectNestedCompositeNodesGraph() {
    // given
    when(actionProvider.get(eq("simpleAction"))).thenReturn(Optional.of(actionMock));

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(
            new GraphNodeOptions(subTasks(new GraphNodeOptions("simpleAction", NO_TRANSITIONS)),
                NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    Task task = getTask(graph);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals("composite", rootNode.getId());

    CompositeNode compositeRootNode = (CompositeNode) rootNode;
    assertEquals(1, compositeRootNode.getNodes().size());
    Node childNode = compositeRootNode.getNodes().get(0);
    assertEquals(COMPOSITE_NODE_ID, childNode.getId());
    assertTrue(childNode instanceof CompositeNode);
    CompositeNode compositeChildNode = (CompositeNode) childNode;

    assertEquals(1, compositeChildNode.getNodes().size());
    Node node = compositeChildNode.getNodes().get(0);
    assertTrue(node instanceof SingleNode);
    assertEquals("simpleAction", node.getId());
  }

  private Task getTask(GraphNodeOptions graph) {
    return new ConfigurationTaskProvider(actionProvider)
        .newInstance(new Configuration(TASK_NAME, graph), SAMPLE_FRAGMENT_EVENT);
  }

  private List<GraphNodeOptions> subTasks(GraphNodeOptions... nodes) {
    return Arrays.asList(nodes);
  }
}