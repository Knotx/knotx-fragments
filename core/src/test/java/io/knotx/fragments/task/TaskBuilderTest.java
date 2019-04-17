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

import static io.knotx.fragments.handler.api.fragment.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.fragment.Action;
import io.knotx.fragments.handler.exception.GraphConfigurationException;
import io.knotx.fragments.handler.options.NodeOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(VertxExtension.class)
class TaskBuilderTest {

  @Test
  @DisplayName("Expect empty graph node when task not defined.")
  void expectEmptyGraphNodeWhenTaskNotConfigured() {
    // given
    TaskBuilder tested = new TaskBuilder(Collections.emptyMap(), mock(ActionProvider.class));

    // when
    Fragment fragment = new Fragment("type", new JsonObject()
        .put(TaskBuilder.TASK_KEY, "not-existing-task"), "body");
    Optional<Task> task = tested.build(fragment);

    // then
    Assertions.assertFalse(task.isPresent());
  }

  @Test
  @DisplayName("Expect exception when action not defined.")
  void expectExceptionWhenActionNotConfigured() {
    // given
    ActionProvider actionProvider = mock(ActionProvider.class);
    Mockito.when(actionProvider.get(Mockito.eq("actionA"))).thenReturn(Optional.empty());

    TaskBuilder tested = new TaskBuilder(Collections.singletonMap("taskA",
        new NodeOptions("actionA", Collections.emptyMap())), actionProvider);

    // when, then
    Fragment fragment = new Fragment("type", new JsonObject().put(TaskBuilder.TASK_KEY, "taskA"),
        "initial body");

    Assertions.assertThrows(GraphConfigurationException.class, () -> tested.build(fragment));
  }

  // ToDo probably to remove
//  @Test
//  @DisplayName("Expect graph node with correct operation.")
//  void expectGraphNode(VertxTestContext testContext) throws Throwable {
//    // given
//    String initialBody = "initial body";
//    String expectedBody = "expected body";
//    Action expectedAction = (fragmentContext, resultHandler) -> {
//      Fragment fragment = fragmentContext.getFragment();
//      FragmentResult result = new FragmentResult(fragment.setBody(expectedBody),
//          FragmentResult.SUCCESS_TRANSITION);
//      Future.succeededFuture(result).setHandler(resultHandler);
//    };
//
//    ActionProvider actionProvider = mock(ActionProvider.class);
//    Mockito.when(actionProvider.get(Mockito.eq("actionA"))).thenReturn(Optional.of(
//        expectedAction));
//    TaskBuilder tested = new TaskBuilder(
//        Collections.singletonMap("taskA", new NodeOptions("actionA", Collections.emptyMap())),
//        actionProvider);
//
//    // when
//    Fragment fragment = new Fragment("type", new JsonObject().put(TaskBuilder.TASK_KEY, "taskA"),
//        initialBody);
//    Optional<Task> task = tested.build(fragment);
//
//    // then
//    assertTrue(task.isPresent());
//    Single<FragmentResult> operationResult = task.get()
//        .getRootNode().get()
//        .doAction(new FragmentContext(fragment, new ClientRequest()));
//
//    operationResult.subscribe(result -> {
//      assertEquals(expectedBody, result.getFragment().getBody());
//      testContext.completeNow();
//    });
//
//    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
//    if (testContext.failed()) {
//      throw testContext.causeOfFailure();
//    }
//  }

  @Test
  @DisplayName("Expect graph of single operation without transitions.")
  void expectSingleOperationNodeGraph() {
    // given
    Action anyAction = Mockito.mock(Action.class);
    ActionProvider actionProvider = mock(ActionProvider.class);
    Mockito.when(actionProvider.get(Mockito.eq("actionA"))).thenReturn(Optional.of(anyAction));

    TaskBuilder tested = new TaskBuilder(
        Collections.singletonMap("task", new NodeOptions("actionA", Collections.emptyMap())),
        actionProvider);

    // when
    Fragment fragment = new Fragment("type", new JsonObject().put(TaskBuilder.TASK_KEY, "task"),
        "some body");
    Optional<Task> optionalTask = tested.build(fragment);

    // then
    assertTrue(optionalTask.isPresent());
    Task task = optionalTask.get();
    assertEquals("task", task.getName());
    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof ActionNode);
    assertEquals("actionA", rootNode.getId());
    assertFalse(rootNode.next(SUCCESS_TRANSITION).isPresent());
  }

  @Test
  @DisplayName("Expect graph node of two single operations with transition between.")
  void expectGraphNodeWithTransition() {
    // given
    Action anyAction = Mockito.mock(Action.class);
    ActionProvider actionProvider = mock(ActionProvider.class);
    Mockito.when(actionProvider.get(Mockito.eq("actionA"))).thenReturn(Optional.of(anyAction));
    Mockito.when(actionProvider.get(Mockito.eq("actionB"))).thenReturn(Optional.of(anyAction));

    TaskBuilder tested = new TaskBuilder(
        Collections.singletonMap("task", new NodeOptions("actionA", Collections
            .singletonMap("customTransition",
                new NodeOptions("actionB", Collections.emptyMap())))),
        actionProvider);

    // when
    Fragment fragment = new Fragment("type", new JsonObject().put(TaskBuilder.TASK_KEY, "task"),
        "some body");
    Optional<Task> optionalTask = tested.build(fragment);

    // then
    assertTrue(optionalTask.isPresent());
    Task task = optionalTask.get();
    assertEquals("task", task.getName());

    assertTrue(task.getRootNode().isPresent());
    Node rootNode = task.getRootNode().get();
    assertTrue(rootNode instanceof ActionNode);
    assertEquals("actionA", rootNode.getId());
    Optional<Node> customNode = rootNode.next("customTransition");
    assertTrue(customNode.isPresent());
    assertTrue(customNode.get() instanceof ActionNode);
    ActionNode customSingleNode = (ActionNode) customNode.get();
    assertEquals("actionB", customSingleNode.getId());
  }

  @Test
  void expectSingleParallelGraph() {

  }

  @Test
  void expectParallelAndSingleGraph() {

  }

  @Test
  void expectNestedParallelGraph() {

  }
}