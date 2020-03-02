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
package io.knotx.fragments.task.factory;

import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.OperationMetadata;
import io.knotx.fragments.engine.TaskMetadata;
import io.knotx.fragments.engine.TaskWithMetadata;
import io.knotx.fragments.engine.api.Task;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.composite.CompositeNode;
import io.knotx.fragments.engine.api.node.single.SingleNode;
import io.knotx.fragments.handler.api.exception.ConfigurationException;
import io.knotx.fragments.task.factory.node.NodeFactoryOptions;
import io.knotx.fragments.task.factory.node.action.ActionNodeFactory;
import io.knotx.fragments.task.factory.node.action.ActionNodeFactoryConfig;
import io.knotx.fragments.task.factory.node.subtasks.SubtasksNodeFactory;
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
  private static final String TEST_ACTION_FACTORY = "test-action";
  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();

  private static final FragmentEventContext SAMPLE_FRAGMENT_EVENT =
      new FragmentEventContext(new FragmentEvent(new Fragment("type",
          new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, TASK_NAME), "body")),
          new ClientRequest());

  private static final String MY_TASK_KEY = "myTaskKey";

  private static final FragmentEventContext SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY =
      new FragmentEventContext(new FragmentEvent(new Fragment("type",
          new JsonObject().put(MY_TASK_KEY, TASK_NAME), "body")),
          new ClientRequest());

  @Test
  @DisplayName("Expect fragment is not accepted when it does not specify a task.")
  void notAcceptFragmentWithoutTask(Vertx vertx) {
    // given
    FragmentEventContext fragmentWithNoTask =
        new FragmentEventContext(
            new FragmentEvent(
                new Fragment("type", new JsonObject(), "body")),
            new ClientRequest()
        );
    DefaultTaskFactory tested = new DefaultTaskFactory()
        .configure(emptyFactoryConfig().toJson(), vertx);

    // when
    boolean accepted = tested.accept(fragmentWithNoTask);

    // then
    assertFalse(accepted);
  }

  @Test
  @DisplayName("Expect fragment is not accepted when it specifies a task but it is not configured")
  void noAcceptFragmentWhenTaskNotConfigured(Vertx vertx) {
    // given
    DefaultTaskFactory tested = new DefaultTaskFactory()
        .configure(emptyFactoryConfig().toJson(), vertx);

    // when
    boolean accepted = tested.accept(SAMPLE_FRAGMENT_EVENT);

    // then
    assertFalse(accepted);
  }

  @Test
  @DisplayName("Expect fragment is accepted when it specifies a task and it is configured")
  void acceptFragment(Vertx vertx) {
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    DefaultTaskFactory tested = new DefaultTaskFactory().configure(
        createTaskFactoryConfig(graph, new JsonObject()).toJson(), vertx);

    // when
    boolean accepted = tested.accept(SAMPLE_FRAGMENT_EVENT);

    // then
    Assertions.assertTrue(accepted);
  }

  @Test
  @DisplayName("Expect fragment is accepted when it specifies a custom task name and it is configured")
  void acceptFragmentWhenCustomTaskName(Vertx vertx) {
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    DefaultTaskFactory tested = new DefaultTaskFactory().configure(
        createTaskFactoryConfig(graph, new JsonObject()).setTaskNameKey(MY_TASK_KEY).toJson(),
        vertx);

    // when
    boolean accepted = tested.accept(SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY);

    // then
    Assertions.assertTrue(accepted);
  }

  @Test
  @DisplayName("Expect task not found exception when a fragment does not define a task.")
  void newInstanceFailedWhenNoTask(Vertx vertx) {
    // given
    FragmentEventContext fragmentWithNoTask =
        new FragmentEventContext(
            new FragmentEvent(
                new Fragment("type", new JsonObject(), "body")),
            new ClientRequest()
        );
    JsonObject actionNodeConfig = createActionNodeConfig("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    Assertions.assertThrows(
        ConfigurationException.class,
        () -> new DefaultTaskFactory()
            .configure(createTaskFactoryConfig(graph, actionNodeConfig).toJson(), vertx)
            .newInstanceWithMetadata(fragmentWithNoTask));
  }

  @Test
  @DisplayName("Expect new task instance when task name is defined and configured.")
  void newInstance(Vertx vertx) {
    // given
    JsonObject actionNodeConfig = createActionNodeConfig("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    TaskWithMetadata task = new DefaultTaskFactory()
        .configure(createTaskFactoryConfig(graph, actionNodeConfig).toJson(), vertx)
        .newInstanceWithMetadata(SAMPLE_FRAGMENT_EVENT);

    // then
    assertEquals(TASK_NAME, task.getTask().getName());
  }

  @Test
  @DisplayName("Expect always a new task instance.")
  void newInstanceAlways(Vertx vertx) {
    // given
    JsonObject actionNodeConfig = createActionNodeConfig("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    DefaultTaskFactory taskFactory = new DefaultTaskFactory()
        .configure(createTaskFactoryConfig(graph, actionNodeConfig).toJson(), vertx);

    // then
    assertNotSame(
        taskFactory.newInstanceWithMetadata(SAMPLE_FRAGMENT_EVENT),
        taskFactory.newInstanceWithMetadata(SAMPLE_FRAGMENT_EVENT)
    );
  }

  @Test
  @DisplayName("Expect new task instance when custom task name key is defined.")
  void expectGraphWhenCustomTaskKey(Vertx vertx) {
    // given
    JsonObject actionNodeConfig = createActionNodeConfig("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    TaskWithMetadata task = new DefaultTaskFactory()
        .configure(
            createTaskFactoryConfig(graph, actionNodeConfig).setTaskNameKey(MY_TASK_KEY).toJson(),
            vertx)
        .newInstanceWithMetadata(SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY);

    // then
    assertEquals(TASK_NAME, task.getTask().getName());
  }

  @Test
  @DisplayName("Expect metadata when custom task name key is defined.")
  void expectMetadataWhenCustomTaskKey(Vertx vertx) {
    // given
    JsonObject actionNodeConfig = createActionNodeConfig("A", SUCCESS_TRANSITION,
        "info");
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    TaskWithMetadata taskWithMetadata = new DefaultTaskFactory()
        .configure(
            createTaskFactoryConfig(graph, actionNodeConfig).setTaskNameKey(MY_TASK_KEY).toJson(),
            vertx)
        .newInstanceWithMetadata(SAMPLE_FRAGMENT_EVENT_WITH_CUSTOM_TASK_KEY);

    JsonObject actionConfig = actionNodeConfig.getJsonObject("actions").getJsonObject("A");

    // then
    TaskMetadata metadata = taskWithMetadata.getTaskMetadata();
    assertEquals(TASK_NAME, metadata.getTaskName());

    NodeMetadata rootMetadata = metadata.getNodesMetadata().get(metadata.getRootNodeId());
    assertTrue(rootMetadata.getTransitions().isEmpty());
    assertTrue(rootMetadata.getNestedNodes().isEmpty());
    assertEquals(NodeType.SINGLE, rootMetadata.getType());

    OperationMetadata operation = rootMetadata.getOperation();
    assertActionNodeMetadata(operation, "A", actionConfig);
  }

  @Test
  @DisplayName("Expect graph of two action nodes with transition between.")
  void expectNodesWithTransitionBetween(Vertx vertx) {
    // given
    JsonObject options = createActionNodeConfig("A", SUCCESS_TRANSITION);
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
    Optional<Node> customNode = rootNode.next("customTransition");
    assertTrue(customNode.isPresent());
    assertTrue(customNode.get() instanceof SingleNode);
    SingleNode customSingleNode = (SingleNode) customNode.get();
  }

  @Test
  @DisplayName("Expect metadata of two action nodes with transition between.")
  void expectMetadataForNodesWithTransitionBetween(Vertx vertx) {
    // given
    JsonObject options = createActionNodeConfig("A", SUCCESS_TRANSITION, "info");
    merge(options, "B", SUCCESS_TRANSITION, "info");

    GraphNodeOptions graph = new GraphNodeOptions("A", Collections
        .singletonMap("customTransition",
            new GraphNodeOptions("B", NO_TRANSITIONS)));

    // when
    TaskMetadata metadata = getTaskWithMetadata(graph, options, vertx).getTaskMetadata();

    JsonObject actionAConfig = options.getJsonObject("actions").getJsonObject("A");
    JsonObject actionBConfig = options.getJsonObject("actions").getJsonObject("B");

    // then
    assertEquals(TASK_NAME, metadata.getTaskName());

    NodeMetadata rootMetadata = metadata.getNodesMetadata().get(metadata.getRootNodeId());
    NodeMetadata nextNodeMetadata = metadata
        .getNodesMetadata().get(rootMetadata.getTransitions().get("customTransition"));

    assertEquals(NodeType.SINGLE, rootMetadata.getType());
    assertEquals(NodeType.SINGLE, nextNodeMetadata.getType());

    assertActionNodeMetadata(rootMetadata.getOperation(), "A", actionAConfig);
    assertActionNodeMetadata(nextNodeMetadata.getOperation(), "B", actionBConfig);
  }

  @Test
  @DisplayName("Expect graph with nested composite nodes")
  void expectNestedCompositeNodesGraph(Vertx vertx) {
    // given
    JsonObject options = createActionNodeConfig("A", SUCCESS_TRANSITION);

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

    CompositeNode compositeRootNode = (CompositeNode) rootNode;
    assertEquals(1, compositeRootNode.getNodes().size());
    Node childNode = compositeRootNode.getNodes().get(0);
    assertTrue(childNode instanceof CompositeNode);
    CompositeNode compositeChildNode = (CompositeNode) childNode;

    assertEquals(1, compositeChildNode.getNodes().size());
    Node node = compositeChildNode.getNodes().get(0);
    assertTrue(node instanceof SingleNode);
  }

  @Test
  @DisplayName("Expect metadata for graph with nested composite nodes")
  void expectMetadataForNestedCompositeNodesGraph(Vertx vertx) {
    // given
    JsonObject options = createActionNodeConfig("A", SUCCESS_TRANSITION, "info");

    GraphNodeOptions graph = new GraphNodeOptions(
        subTasks(
            new GraphNodeOptions(subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
                NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    TaskMetadata metadata = getTaskWithMetadata(graph, options, vertx).getTaskMetadata();

    JsonObject actionConfig = options.getJsonObject("actions").getJsonObject("A");

    NodeMetadata rootMetadata = metadata.getNodesMetadata().get(metadata.getRootNodeId());
    NodeMetadata nestedNodeMetadata = metadata.getNodesMetadata()
        .get(rootMetadata.getNestedNodes().get(0));
    NodeMetadata doubleNestedNodeMetadata = metadata.getNodesMetadata()
        .get(nestedNodeMetadata.getNestedNodes().get(0));

    assertEquals(NodeType.COMPOSITE, rootMetadata.getType());
    assertEquals(NodeType.COMPOSITE, nestedNodeMetadata.getType());
    assertEquals(NodeType.SINGLE, doubleNestedNodeMetadata.getType());

    assertActionNodeMetadata(doubleNestedNodeMetadata.getOperation(), "A", actionConfig);
  }

  private Task getTask(GraphNodeOptions graph, JsonObject actionNodeConfig, Vertx vertx) {
    DefaultTaskFactoryConfig taskFactoryConfig = createTaskFactoryConfig(graph, actionNodeConfig);
    return new DefaultTaskFactory().configure(taskFactoryConfig.toJson(), vertx)
        .newInstance(SAMPLE_FRAGMENT_EVENT);
  }

  private TaskWithMetadata getTaskWithMetadata(GraphNodeOptions graph, JsonObject actionNodeConfig,
      Vertx vertx) {
    DefaultTaskFactoryConfig taskFactoryConfig = createTaskFactoryConfig(graph, actionNodeConfig);
    return new DefaultTaskFactory().configure(taskFactoryConfig.toJson(), vertx)
        .newInstanceWithMetadata(SAMPLE_FRAGMENT_EVENT);
  }

  private DefaultTaskFactoryConfig emptyFactoryConfig() {
    return createTaskFactoryConfig(null, null);
  }

  private DefaultTaskFactoryConfig createTaskFactoryConfig(GraphNodeOptions graph,
      JsonObject actionNodeConfig) {
    DefaultTaskFactoryConfig taskFactoryConfig = new DefaultTaskFactoryConfig();
    if (graph != null) {
      taskFactoryConfig
          .setTasks(Collections.singletonMap(TASK_NAME, graph));
    }
    if (actionNodeConfig != null) {
      List<NodeFactoryOptions> nodeFactories = Arrays.asList(
          new NodeFactoryOptions().setFactory(ActionNodeFactory.NAME).setConfig(actionNodeConfig),
          new NodeFactoryOptions().setFactory(SubtasksNodeFactory.NAME));
      taskFactoryConfig.setNodeFactories(nodeFactories);
    }
    return taskFactoryConfig;
  }

  private JsonObject createActionNodeConfig(String actionName, String transition) {
    JsonObject actionConfig = new JsonObject().put("transition", transition);
    return createActionNodeConfig(actionName, actionConfig);
  }

  private JsonObject createActionNodeConfig(String actionName, String transition,
      String logLevel) {
    JsonObject actionConfig = new JsonObject().put("transition", transition)
        .put("logLevel", logLevel);
    return createActionNodeConfig(actionName, actionConfig);
  }

  private JsonObject createActionNodeConfig(String actionName, JsonObject actionConfig) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionFactoryOptions(new JsonObject())
            .setFactory(TEST_ACTION_FACTORY)
            .setConfig(actionConfig)))
        .toJson();
  }

  private void merge(JsonObject current, String actionName, String transition) {
    JsonObject newOptions = createActionNodeConfig(actionName, transition);
    current.getJsonObject("actions").mergeIn(newOptions.getJsonObject("actions"));
  }

  private void merge(JsonObject current, String actionName, String transition,
      String logLevel) {
    JsonObject newOptions = createActionNodeConfig(actionName, transition, logLevel);
    current.getJsonObject("actions").mergeIn(newOptions.getJsonObject("actions"));
  }

  private List<GraphNodeOptions> subTasks(GraphNodeOptions... nodes) {
    return Arrays.asList(nodes);
  }

  private void assertActionNodeMetadata(OperationMetadata operation, String actionName,
      JsonObject actionConfig) {
    assertEquals("action", operation.getFactory());
    assertEquals(actionName, operation.getData().getString("alias"));
    assertEquals(TEST_ACTION_FACTORY, operation.getData().getString("actionFactory"));
    assertEquals(actionConfig, operation.getData().getJsonObject("actionConfig"));
  }
}
