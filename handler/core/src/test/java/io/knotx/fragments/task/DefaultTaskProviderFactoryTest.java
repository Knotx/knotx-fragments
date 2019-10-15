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

import static io.knotx.fragments.engine.graph.CompositeNode.COMPOSITE_NODE_ID;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.handler.options.FragmentsHandlerOptions.DEFAULT_TASK_KEY;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.exception.GraphConfigurationException;
import io.knotx.fragments.handler.options.NodeOptions;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultTaskProviderFactoryTest {

  private static final Map<String, NodeOptions> NO_TRANSITIONS = Collections.emptyMap();

  private static final String TASK_NAME = "task";
  private static final Fragment FRAGMENT =
      new Fragment("type",
          new JsonObject().put(DEFAULT_TASK_KEY, TASK_NAME),
          "initial body");

  @Mock
  private ActionProvider actionProvider;

  @Mock
  Action actionMock;

  @Test
  @DisplayName("Expect empty task when empty tasks.")
  void expectNoTaskWhenEmptyTasks() {
    // given
    TaskProvider tested = getTested(Collections.emptyMap());

    // when
    Optional<Task> task = tested.get(FRAGMENT);

    // then
    Assertions.assertFalse(task.isPresent());
  }

  @Test
  @DisplayName("Expect configuration exception when action not defined.")
  void expectExceptionWhenActionNotConfigured() {
    // given
    when(actionProvider.get(eq("nonExistingAction"))).thenReturn(Optional.empty());
    TaskProvider tested = getTested(singletonMap(TASK_NAME,
        new NodeOptions("nonExistingAction", NO_TRANSITIONS)));

    // when, then
    Assertions.assertThrows(GraphConfigurationException.class, () -> tested.get(FRAGMENT));
  }

  @ParameterizedTest
  @ValueSource(strings = {DEFAULT_TASK_KEY, "custom-task-key"})
  @DisplayName("Expect task name when task key.")
  void expectTaskName(String taskKey) {
    // given
    when(actionProvider.get(eq("A"))).thenReturn(Optional.of(actionMock));
    TaskProvider tested = new DefaultTaskProviderFactory()
        .create(taskKey, singletonMap(TASK_NAME, new NodeOptions("A", NO_TRANSITIONS)),
            actionProvider);

    // when
    Optional<Task> task = tested.get(new Fragment("type",
        new JsonObject().put(taskKey, TASK_NAME), "initial body"));

    // then
    assertTrue(task.isPresent());
    assertEquals(TASK_NAME, task.get().getName());
  }

  @Test
  @DisplayName("Expect task: A.")
  void expectActionNodeWithNoTransitions() {
    // given
    when(actionProvider.get(eq("A"))).thenReturn(Optional.of(actionMock));
    TaskProvider tested = getTested(
        singletonMap(TASK_NAME, new NodeOptions("A", NO_TRANSITIONS)));

    // when
    Optional<Task> task = tested.get(FRAGMENT);

    // then
    assertTrue(task.isPresent());
    assertTrue(task.get().getRootNode().isPresent());
    Node rootNode = task.get().getRootNode().get();
    assertTrue(rootNode instanceof ActionNode);
    assertEquals("A", rootNode.getId());
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
    assertFalse(rootNode.next(ERROR_TRANSITION).isPresent());
  }

  @Test
  @DisplayName("Expect task: A - _success -> B.")
  void expectActionNodeNextActionNode() {
    // given
    when(actionProvider.get("A")).thenReturn(Optional.of(actionMock));
    when(actionProvider.get("B")).thenReturn(Optional.of(actionMock));

    TaskProvider tested = getTested(
        singletonMap(TASK_NAME, new NodeOptions("A", singletonMap(SUCCESS_TRANSITION,
            new NodeOptions("B", NO_TRANSITIONS)))));

    // when
    Optional<Task> task = tested.get(FRAGMENT);

    // then
    assertTrue(task.isPresent());
    assertTrue(task.get().getRootNode().isPresent());
    Node aNode = task.get().getRootNode().get();
    assertTrue(aNode instanceof ActionNode);
    assertEquals("A", aNode.getId());
    Optional<Node> bNode = aNode.next(SUCCESS_TRANSITION);
    assertTrue(bNode.isPresent());
    assertTrue(bNode.get() instanceof ActionNode);
    assertEquals("B", bNode.get().getId());
  }

  @Test
  @DisplayName("Expect composite node: (A).")
  void expectCompositeNodeWithNoTransitions() {
    // given
    when(actionProvider.get(eq("A"))).thenReturn(Optional.of(actionMock));

    TaskProvider tested = getTested(singletonMap(TASK_NAME,
        new NodeOptions(
            actions(new NodeOptions("A", NO_TRANSITIONS)),
            NO_TRANSITIONS
        ))
    );

    // when
    Optional<Task> task = tested.get(FRAGMENT);

    // then
    assertTrue(task.isPresent());
    assertTrue(task.get().getRootNode().isPresent());
    Node compositeNode = task.get().getRootNode().get();
    assertTrue(compositeNode instanceof CompositeNode);
    assertEquals(COMPOSITE_NODE_ID, compositeNode.getId());

    List<Node> compositeNodes = ((CompositeNode) compositeNode).getNodes();
    assertEquals(1, compositeNodes.size());
    assertEquals("A", compositeNodes.get(0).getId());
    assertTrue(compositeNodes.get(0) instanceof ActionNode);
  }

  @Test
  @DisplayName("Expect B node: (A) - _success -> B.")
  void expectCompositeNodeWithSingleNodeOnSuccessGraph() {
    // given
    when(actionProvider.get(eq("A"))).thenReturn(Optional.of(actionMock));
    when(actionProvider.get(eq("B"))).thenReturn(Optional.of(actionMock));

    TaskProvider tested = getTested(
        singletonMap(TASK_NAME,
            new NodeOptions(
                actions(new NodeOptions("A", NO_TRANSITIONS)),
                singletonMap(SUCCESS_TRANSITION,
                    new NodeOptions("B", NO_TRANSITIONS))
            )));

    // when
    Optional<Task> task = tested.get(FRAGMENT);

    // then
    assertTrue(task.isPresent());
    assertTrue(task.get().getRootNode().isPresent());
    Node compositeNode = task.get().getRootNode().get();
    Optional<Node> bNode = compositeNode.next(SUCCESS_TRANSITION);
    assertTrue(bNode.isPresent());
    assertEquals("B", bNode.get().getId());
  }

  @Test
  @DisplayName("Expect task: (A) - _custom (invalid) -> B.")
  void expectCompositeNodeAcceptsOnlySuccessAndErrorTransitions() {
    // given
    when(actionProvider.get(eq("A"))).thenReturn(Optional.of(actionMock));
    when(actionProvider.get(eq("B"))).thenReturn(Optional.of(actionMock));

    TaskProvider tested = getTested(
        singletonMap(TASK_NAME,
            new NodeOptions(
                actions(new NodeOptions("A", NO_TRANSITIONS)),
                singletonMap("_custom",
                    new NodeOptions("B", NO_TRANSITIONS))
            )));

    // when
    Optional<Task> task = tested.get(FRAGMENT);

    // then
    assertTrue(task.isPresent());
    assertTrue(task.get().getRootNode().isPresent());
    Node rootNode = task.get().getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
    assertFalse(rootNode.next(ERROR_TRANSITION).isPresent());
    assertFalse(rootNode.next("_custom").isPresent());
  }

  @Test
  @DisplayName("Expect task: [[A]]")
  void expectNestedCompositeNodesGraph() {
    // given
    when(actionProvider.get(eq("A"))).thenReturn(Optional.of(actionMock));

    TaskProvider tested = getTested(
        singletonMap(TASK_NAME,
            new NodeOptions(
                actions(
                    new NodeOptions(
                        actions(new NodeOptions("A", NO_TRANSITIONS)),
                        NO_TRANSITIONS)),
                NO_TRANSITIONS
            )));

    // when
    Optional<Task> optionalTask = tested.get(FRAGMENT);

    // then
    assertTrue(optionalTask.isPresent());
    Task task = optionalTask.get();
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof CompositeNode);

    CompositeNode compositeRootNode = (CompositeNode) rootNode;
    Node childNode = compositeRootNode.getNodes().get(0);
    CompositeNode compositeChildNode = (CompositeNode) childNode;
    Node aNode = compositeChildNode.getNodes().get(0);
    assertTrue(aNode instanceof ActionNode);
    assertEquals("A", aNode.getId());
  }


  private TaskProvider getTested(Map<String, NodeOptions> tasks) {
    return new DefaultTaskProviderFactory().create(DEFAULT_TASK_KEY, tasks, actionProvider);
  }

  private List<NodeOptions> actions(NodeOptions... nodes) {
    return Arrays.asList(nodes);
  }
}