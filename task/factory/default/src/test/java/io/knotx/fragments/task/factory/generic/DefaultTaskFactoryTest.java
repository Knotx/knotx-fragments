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
package io.knotx.fragments.task.factory.generic;

import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.task.factory.generic.DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.task.api.composite.CompositeNode;
import io.knotx.fragments.task.api.single.SingleNode;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.knotx.fragments.task.factory.api.metadata.TaskMetadata;
import io.knotx.fragments.task.factory.api.metadata.TaskWithMetadata;
import io.knotx.fragments.task.factory.generic.node.NodeFactoryOptions;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactory;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactoryConfig;
import io.knotx.fragments.task.factory.generic.node.subtasks.SubtasksNodeFactory;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

  private static final String MY_TASK_KEY = "myTaskKey";
  private static final String TASK_NAME = "task";
  private static final String TEST_ACTION_FACTORY = "test-action";
  private static final Map<String, GraphNodeOptions> NO_TRANSITIONS = Collections.emptyMap();

  private Fragment fragment;
  private Fragment fragmentWithCustomTaskKey;

  @BeforeEach
  void configure() {
    fragment = new Fragment("type", new JsonObject().put(DEFAULT_TASK_NAME_KEY, TASK_NAME), "body");
    fragmentWithCustomTaskKey = new Fragment("type", new JsonObject().put(MY_TASK_KEY, TASK_NAME),
        "body");
  }

  @Test
  @DisplayName("Fragment is not accepted when it does not specify a task.")
  void notAcceptFragmentWithoutTask(Vertx vertx) {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), "body");

    DefaultTaskFactory tested = new DefaultTaskFactory()
        .configure(taskFactoryConfig().toJson(), vertx);

    // when
    boolean accepted = tested.accept(fragment, new ClientRequest());

    // then
    assertFalse(accepted);
  }

  @Test
  @DisplayName("Fragment is not accepted when it specifies a task but it is not configured.")
  void notAcceptFragmentWhenTaskNotConfigured(Vertx vertx) {
    // given
    DefaultTaskFactory tested = new DefaultTaskFactory()
        .configure(taskFactoryConfig().toJson(), vertx);

    // when
    boolean accepted = tested.accept(fragment, new ClientRequest());

    // then
    assertFalse(accepted);
  }

  @Test
  @DisplayName("Fragment is accepted when it specifies a task and it is configured.")
  void acceptFragment(Vertx vertx) {
    // given
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);

    DefaultTaskFactory tested = new DefaultTaskFactory().configure(
        taskFactoryConfig(rootNodeOptions).toJson(), vertx);

    // when
    boolean accepted = tested.accept(fragment, new ClientRequest());

    // then
    assertTrue(accepted);
  }

  @Test
  @DisplayName("Fragment is accepted when it specifies a custom task name.")
  void acceptFragmentWhenCustomTaskName(Vertx vertx) {
    // given
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);

    DefaultTaskFactory tested = new DefaultTaskFactory().configure(
        taskFactoryConfig(rootNodeOptions).setTaskNameKey(MY_TASK_KEY).toJson(),
        vertx);

    // when
    boolean accepted = tested.accept(fragmentWithCustomTaskKey, new ClientRequest());

    // then
    assertTrue(accepted);
  }

  @Test
  @DisplayName("Configuration exception is thrown when a fragment does not define a task.")
  void newInstanceFailedWhenNoTask(Vertx vertx) {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), "body");
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);
    DefaultTaskFactory tested = new DefaultTaskFactory()
        .configure(taskFactoryConfig(rootNodeOptions, actionNodeFactoryConfig("A")).toJson(),
            vertx);

    // when, then
    assertThrows(
        IllegalArgumentException.class,
        () -> tested.newInstance(fragment, new ClientRequest()));
  }

  @Test
  @DisplayName("Expect new task instance when task name is defined and configured.")
  void newInstance(Vertx vertx) {
    // given
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    TaskWithMetadata task = new DefaultTaskFactory()
        .configure(taskFactoryConfig(rootNodeOptions, actionNodeFactoryConfig("A")).toJson(), vertx)
        .newInstance(fragment, new ClientRequest());

    // then
    assertEquals(TASK_NAME, task.getTask().getName());
  }

  @Test
  @DisplayName("Expect always a new task instance.")
  void newInstanceAlways(Vertx vertx) {
    // given
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    DefaultTaskFactory taskFactory = new DefaultTaskFactory()
        .configure(taskFactoryConfig(rootNodeOptions, actionNodeFactoryConfig("A")).toJson(),
            vertx);

    // then
    assertNotSame(
        taskFactory.newInstance(fragment, new ClientRequest()),
        taskFactory.newInstance(fragment, new ClientRequest())
    );
  }

  @Test
  @DisplayName("Expect new task instance when custom task name key is defined.")
  void expectGraphWhenCustomTaskKey(Vertx vertx) {
    // given
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    TaskWithMetadata task = new DefaultTaskFactory()
        .configure(
            taskFactoryConfig(rootNodeOptions, actionNodeFactoryConfig("A"))
                .setTaskNameKey(MY_TASK_KEY)
                .toJson(),
            vertx)
        .newInstance(fragmentWithCustomTaskKey, new ClientRequest());

    // then
    assertEquals(TASK_NAME, task.getTask().getName());
  }

  @Test
  @DisplayName("Expect task metadata when INFO is configured.")
  void expectMetadata(Vertx vertx) {
    // given
    JsonObject actionNodeConfig = infoLevel(actionNodeFactoryConfig("A"));
    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", NO_TRANSITIONS);

    // when
    TaskWithMetadata taskWithMetadata = new DefaultTaskFactory()
        .configure(
            taskFactoryConfig(rootNodeOptions, actionNodeConfig)
                .toJson(),
            vertx)
        .newInstance(fragment, new ClientRequest());

    ActionFactoryOptions actionOptions = new ActionNodeFactoryConfig(actionNodeConfig).getActions()
        .get("A");

    // then
    TaskMetadata metadata = taskWithMetadata.getMetadata();
    assertEquals(TASK_NAME, metadata.getTaskName());

    NodeMetadata rootMetadata = metadata.getNodesMetadata().get(metadata.getRootNodeId());
    assertTrue(rootMetadata.getTransitions().isEmpty());
    assertTrue(rootMetadata.getNestedNodes().isEmpty());
    assertEquals(NodeType.SINGLE, rootMetadata.getType());

    OperationMetadata operation = rootMetadata.getOperation();
    assertActionNodeMetadata(operation, "A", actionOptions.getConfig());
  }

  @Test
  @DisplayName("Expect graph of two action nodes with transition between.")
  void expectNodesWithTransitionBetween(Vertx vertx) {
    // given
    JsonObject options = actionNodeFactoryConfig("A", "B");

    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A",
        singletonMap("customTransition",
            new GraphNodeOptions("B", NO_TRANSITIONS)));

    // when
    TaskWithMetadata taskWithMetadata = getTaskWithMetadata(rootNodeOptions, options, vertx);

    // then
    assertEquals(TASK_NAME, taskWithMetadata.getTask().getName());

    assertTrue(taskWithMetadata.getTask().getRootNode().isPresent());
    Node rootNode = taskWithMetadata.getTask().getRootNode().get();
    Optional<Node> customNode = rootNode.next("customTransition");
    assertTrue(customNode.isPresent());
  }

  @Test
  @DisplayName("Expect metadata of two action nodes with transition between.")
  void expectMetadataForNodesWithTransitionBetween(Vertx vertx) {
    // given
    JsonObject options = infoLevel(actionNodeFactoryConfig("A", "B"));

    GraphNodeOptions rootNodeOptions = new GraphNodeOptions("A", singletonMap("customTransition",
        new GraphNodeOptions("B", NO_TRANSITIONS)));

    // when
    TaskMetadata metadata = getTaskWithMetadata(rootNodeOptions, options, vertx).getMetadata();

    // then
    assertEquals(TASK_NAME, metadata.getTaskName());

    NodeMetadata rootMetadata = metadata.getNodesMetadata().get(metadata.getRootNodeId());
    NodeMetadata nextNodeMetadata = metadata
        .getNodesMetadata().get(rootMetadata.getTransitions().get("customTransition"));

    Map<String, ActionFactoryOptions> actions = new ActionNodeFactoryConfig(options).getActions();
    assertActionNodeMetadata(rootMetadata.getOperation(), "A", actions.get("A").getConfig());
    assertActionNodeMetadata(nextNodeMetadata.getOperation(), "B", actions.get("B").getConfig());
  }

  @Test
  @DisplayName("Expect graph with nested composite nodes")
  void expectNestedCompositeNodesGraph(Vertx vertx) {
    // given
    JsonObject options = actionNodeFactoryConfig("A");

    GraphNodeOptions rootNodeOptions = new GraphNodeOptions(
        subTasks(
            new GraphNodeOptions(subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
                NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    TaskWithMetadata taskWithMetadata = getTaskWithMetadata(rootNodeOptions, options, vertx);

    // then
    assertEquals(TASK_NAME, taskWithMetadata.getTask().getName());
    assertTrue(taskWithMetadata.getTask().getRootNode().isPresent());
    Node rootNode = taskWithMetadata.getTask().getRootNode().get();
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
    JsonObject options = infoLevel(actionNodeFactoryConfig("A"));

    GraphNodeOptions rootNodeOptions = new GraphNodeOptions(
        subTasks(
            new GraphNodeOptions(subTasks(new GraphNodeOptions("A", NO_TRANSITIONS)),
                NO_TRANSITIONS)),
        NO_TRANSITIONS
    );

    // when
    TaskMetadata metadata = getTaskWithMetadata(rootNodeOptions, options, vertx).getMetadata();

    Map<String, ActionFactoryOptions> actions = new ActionNodeFactoryConfig(options).getActions();

    NodeMetadata rootMetadata = metadata.getNodesMetadata().get(metadata.getRootNodeId());
    NodeMetadata nestedNodeMetadata = metadata.getNodesMetadata()
        .get(rootMetadata.getNestedNodes().get(0));
    NodeMetadata doubleNestedNodeMetadata = metadata.getNodesMetadata()
        .get(nestedNodeMetadata.getNestedNodes().get(0));

    assertEquals(NodeType.COMPOSITE, rootMetadata.getType());
    assertEquals(NodeType.COMPOSITE, nestedNodeMetadata.getType());
    assertEquals(NodeType.SINGLE, doubleNestedNodeMetadata.getType());

    assertActionNodeMetadata(doubleNestedNodeMetadata.getOperation(), "A",
        actions.get("A").getConfig());
  }

  private TaskWithMetadata getTaskWithMetadata(GraphNodeOptions graph, JsonObject actionNodeConfig,
      Vertx vertx) {
    DefaultTaskFactoryConfig taskFactoryConfig = taskFactoryConfig(graph, actionNodeConfig);
    return new DefaultTaskFactory().configure(taskFactoryConfig.toJson(), vertx)
        .newInstance(fragment, new ClientRequest());
  }

  private DefaultTaskFactoryConfig taskFactoryConfig() {
    return taskFactoryConfig(null, null);
  }

  private DefaultTaskFactoryConfig taskFactoryConfig(GraphNodeOptions rootNodeOptions) {
    return taskFactoryConfig(rootNodeOptions, new JsonObject());
  }

  private DefaultTaskFactoryConfig taskFactoryConfig(GraphNodeOptions rootNodeOptions,
      JsonObject actionNodeFactoryConfig) {
    DefaultTaskFactoryConfig taskFactoryConfig = new DefaultTaskFactoryConfig();
    if (rootNodeOptions != null) {
      taskFactoryConfig
          .setTasks(singletonMap(TASK_NAME, rootNodeOptions));
    }
    if (actionNodeFactoryConfig != null) {
      List<NodeFactoryOptions> nodeFactories = asList(
          new NodeFactoryOptions().setFactory(ActionNodeFactory.NAME)
              .setConfig(actionNodeFactoryConfig),
          new NodeFactoryOptions().setFactory(SubtasksNodeFactory.NAME));
      taskFactoryConfig.setNodeFactories(nodeFactories);
    }
    return taskFactoryConfig;
  }


  private JsonObject actionNodeFactoryConfig(String... actionNames) {
    Map<String, ActionFactoryOptions> actionToOptions = new HashMap<>();
    asList(actionNames).forEach(actionName -> actionToOptions.put(actionName,
        new ActionFactoryOptions(TEST_ACTION_FACTORY, new JsonObject(), null)
            .setConfig(new JsonObject().put("transition", SUCCESS_TRANSITION))));
    return new ActionNodeFactoryConfig(actionToOptions).toJson();
  }

  private JsonObject infoLevel(JsonObject nodeFactoryConfig) {
    ActionNodeFactoryConfig original = new ActionNodeFactoryConfig(nodeFactoryConfig);
    return new ActionNodeFactoryConfig(original.getActions(), ActionLogLevel.INFO).toJson();
  }

  private List<GraphNodeOptions> subTasks(GraphNodeOptions... nodes) {
    return asList(nodes);
  }

  private void assertActionNodeMetadata(OperationMetadata operation, String actionName,
      JsonObject actionConfig) {
    assertEquals("action", operation.getFactory());
    assertEquals(actionName, operation.getData().getString("alias"));
    assertEquals(TEST_ACTION_FACTORY, operation.getData().getString("actionFactory"));
    assertEquals(actionConfig, operation.getData().getJsonObject("actionConfig"));
  }
}
