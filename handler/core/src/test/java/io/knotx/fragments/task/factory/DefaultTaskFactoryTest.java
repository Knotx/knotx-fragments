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
package io.knotx.fragments.task.factory;

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.task.exception.NodeConfigException;
import io.knotx.fragments.task.exception.NodeGraphException;
import io.knotx.fragments.task.factory.config.ActionsConfig;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.fragments.task.options.TaskOptions;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultTaskFactoryTest {

  private static final String TASK_NAME = "task";
  private static final String COMPOSITE_NODE_ID = "composite";
  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();

  private static final FragmentEventContext SAMPLE_FRAGMENT_EVENT =
      new FragmentEventContext(new FragmentEvent(new Fragment("type",
          new JsonObject().put(DefaultTaskFactory.DEFAULT_TASK_NAME_KEY, TASK_NAME), "body")),
          new ClientRequest());

  private static final String MY_TASK_KEY = "myTaskKey";

  private static final FragmentEventContext SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY =
      new FragmentEventContext(new FragmentEvent(new Fragment("type",
          new JsonObject().put(MY_TASK_KEY, TASK_NAME), "body")),
          new ClientRequest());

  @Test
  @DisplayName("Expect graph when custom task key is defined.")
  void expectGraphWhenCustomTaskKey(Vertx vertx) {
    // given
    JsonObject options = options("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    JsonObject factoryOptions = new JsonObject();
    factoryOptions.mergeIn(options);
    factoryOptions
        .put("tasks", new JsonObject().put(TASK_NAME, new TaskOptions().setGraph(graph).toJson()));
    factoryOptions.put("taskNameKey", MY_TASK_KEY);

    // when
    Task task = new DefaultTaskFactory().configure(factoryOptions)
        .newInstance(SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());
  }

  @Test
  @DisplayName("Expect exception when `actions` not defined.")
  void expectExceptionWhenActionsNotConfigured(Vertx vertx) {
    // given
    JsonObject options = new JsonObject();
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when, then
    Assertions.assertThrows(
        NodeConfigException.class, () -> getTask(graph, options, vertx));
  }

  @Test
  @DisplayName("Expect exception when action not defined.")
  void expectExceptionWhenActionNotConfigured(Vertx vertx) {
    // given
    JsonObject options = new JsonObject().put("actions", new JsonObject());
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when, then
    Assertions.assertThrows(
        NodeGraphException.class, () -> getTask(graph, options, vertx));
  }


  @Test
  @DisplayName("Expect graph with single action node without transitions.")
  void expectSingleActionNodeGraph(Vertx vertx) {
    // given
    JsonObject options = options("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    Task task = getTask(graph, options, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof SingleNode);
    assertEquals("A", rootNode.getId());
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
  }

  @Test
  @DisplayName("Expect graph of two action nodes with transition between.")
  void expectActionNodesGraphWithTransition(Vertx vertx) {
    // given
    JsonObject options = options("A", SUCCESS_TRANSITION);
    merge(options, "B", SUCCESS_TRANSITION);

    GraphNodeOptions graph = new GraphNodeOptions("A", Collections
        .singletonMap("customTransition",
            new GraphNodeOptions("B", NO_TRANSITIONS)));

    // when
    Task task = getTask(graph, options, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());

    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof SingleNode);
    assertEquals("A", rootNode.getId());
    Optional<Node> customNode = rootNode.next("customTransition");
    assertTrue(customNode.isPresent());
    assertTrue(customNode.get() instanceof SingleNode);
    SingleNode customSingleNode = (SingleNode) customNode.get();
    assertEquals("B", customSingleNode.getId());
  }

  @Test
  @DisplayName("Expect graph with single composite node without transitions.")
  void expectSingleCompositeNodeGraphWithNoEdges(Vertx vertx) {
    // given
    JsonObject options = options("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    Task task = getTask(graph, options, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals(COMPOSITE_NODE_ID, rootNode.getId());
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
    assertFalse(rootNode.next(ERROR_TRANSITION).isPresent());

    CompositeNode compositeRootNode = (CompositeNode) rootNode;
    assertEquals(1, compositeRootNode.getNodes().size());
    Node node = compositeRootNode.getNodes().get(0);
    assertTrue(node instanceof SingleNode);
    assertEquals("A", node.getId());
  }


  @Test
  @DisplayName("Expect graph with composite node and success transition to action node.")
  void expectCompositeNodeWithSingleNodeOnSuccessGraph(Vertx vertx) {
    // given
    JsonObject options = options("A", SUCCESS_TRANSITION);
    merge(options, "B", SUCCESS_TRANSITION);

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
        Collections
            .singletonMap(SUCCESS_TRANSITION, new GraphNodeOptions("B", NO_TRANSITIONS))
    );

    // when
    Task task = getTask(graph, options, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals(COMPOSITE_NODE_ID, rootNode.getId());
    Optional<Node> onSuccess = rootNode.next(SUCCESS_TRANSITION);
    assertTrue(onSuccess.isPresent());
    Node onSuccessNode = onSuccess.get();
    assertTrue(onSuccessNode instanceof SingleNode);
    assertEquals("B", onSuccessNode.getId());
  }

  @Test
  @DisplayName("Expect graph with composite node and error transition to action node.")
  void expectCompositeNodeWithSingleNodeOnErrorGraph(Vertx vertx) {
    // given
    JsonObject options = options("A", ERROR_TRANSITION);
    merge(options, "fallback", SUCCESS_TRANSITION);

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
        Collections.singletonMap(ERROR_TRANSITION,
            new GraphNodeOptions("fallback", NO_TRANSITIONS))
    );

    // when
    Task task = getTask(graph, options, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals(COMPOSITE_NODE_ID, rootNode.getId());
    Optional<Node> onError = rootNode.next(ERROR_TRANSITION);
    assertTrue(onError.isPresent());
    Node onErrorNode = onError.get();
    assertTrue(onErrorNode instanceof SingleNode);
    assertEquals("fallback", onErrorNode.getId());
  }

  @Test
  void expectCompositeNodeAcceptsOnlySuccessAndErrorTransitions(Vertx vertx) {
    // given
    JsonObject options = options("A", "customTransition");
    merge(options, "B", SUCCESS_TRANSITION);

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
        Collections
            .singletonMap("customTransition", new GraphNodeOptions("B", NO_TRANSITIONS))
    );

    // when
    Task task = getTask(graph, options, vertx);

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
  void expectNestedCompositeNodesGraph(Vertx vertx) {
    // given
    JsonObject options = options("A", SUCCESS_TRANSITION);

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(
            new GraphNodeOptions(subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
                NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    Task task = getTask(graph, options, vertx);

    // then
    assertEquals(TASK_NAME, task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertEquals(COMPOSITE_NODE_ID, rootNode.getId());

    CompositeNode compositeRootNode = (CompositeNode) rootNode;
    assertEquals(1, compositeRootNode.getNodes().size());
    Node childNode = compositeRootNode.getNodes().get(0);
    assertEquals(COMPOSITE_NODE_ID, childNode.getId());
    assertTrue(childNode instanceof CompositeNode);
    CompositeNode compositeChildNode = (CompositeNode) childNode;

    assertEquals(1, compositeChildNode.getNodes().size());
    Node node = compositeChildNode.getNodes().get(0);
    assertTrue(node instanceof SingleNode);
    assertEquals("A", node.getId());
  }

  private Task getTask(GraphNodeOptions graph, JsonObject actions, Vertx vertx) {
    JsonObject factoryOptions = new JsonObject();
    factoryOptions.mergeIn(actions);
    factoryOptions
        .put("tasks", new JsonObject().put(TASK_NAME, new TaskOptions().setGraph(graph).toJson()));

    return new DefaultTaskFactory().configure(factoryOptions)
        .newInstance(SAMPLE_FRAGMENT_EVENT, vertx);
  }

  private JsonObject options(String actionName, String transition) {
    return new ActionsConfig(Collections.singletonMap(actionName,
        new ActionOptions(new JsonObject())
            .setFactory("test-action")
            .setConfig(new JsonObject().put("transition", transition))))
        .toJson();
  }

  private JsonObject merge(JsonObject current, String actionName, String transition) {
    JsonObject newOptions = options(actionName, transition);
    return current.getJsonObject("actions").mergeIn(newOptions.getJsonObject("actions"));
  }

  private List<GraphNodeOptions> subTasks(GraphNodeOptions... nodes) {
    return Arrays.asList(nodes);
  }
}
