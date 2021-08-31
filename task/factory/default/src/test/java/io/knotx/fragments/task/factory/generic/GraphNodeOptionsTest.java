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

import static io.knotx.junit5.util.HoconLoader.verify;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.*;

import io.knotx.fragments.task.factory.generic.node.action.ActionNodeConfig;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactory;
import io.knotx.fragments.task.factory.generic.node.subtasks.SubtasksNodeConfig;
import io.knotx.fragments.task.factory.generic.node.subtasks.SubtasksNodeFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class GraphNodeOptionsTest {

  @Test
  @DisplayName("Expect task with action node when action defined directly in the task.")
  void expectActionNodeWhenActionDirectlyDefinedInGraph(Vertx vertx) throws Throwable {
    verify("conf/taskWithActionNode.conf", validateActionNode(), vertx);
  }

  @Test
  @DisplayName("Expect task with Action node when action is configured.")
  void expectActionNodeWhenActionDefined(Vertx vertx) throws Throwable {
    verify("conf/taskWithActionNode-fullSyntax.conf", validateActionNode(), vertx);
  }

  @Test
  @DisplayName("Expect subtasks node when actions is configured")
  void expectSubTaskNodeWhenActionsDefined(Vertx vertx) throws Throwable {
    verify("conf/taskWithSubtasksDeprecated.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect subtasks node when subtasks directly is configured")
  void expectSubtasksNodeWhenSubtasksDirectlyDefined(Vertx vertx) throws Throwable {
    verify("conf/taskWithSubtasks.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect subtasks node when subtasks is configured")
  void expectSubtasksNodeWhenSubtasksDefined(Vertx vertx) throws Throwable {
    verify("conf/taskWithSubtasks-fullSyntax.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect graph with nested composite nodes")
  void expectDefaultGlobalLogLevel(Vertx vertx) throws Throwable {
    verify("conf/taskWithSubtasks-fullSyntax.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect nodes configured with _success flow")
  void expectTransitionSuccessWithNodeBThenNodeC(Vertx vertx) throws Throwable {
    verify("conf/taskWithTransitions.conf", config -> {
      GraphNodeOptions graphNodeOptions = new GraphNodeOptions(config);
      Optional<GraphNodeOptions> nodeB = graphNodeOptions.get(SUCCESS_TRANSITION);
      assertTrue(nodeB.isPresent());
      assertEquals("b", getAction(nodeB.get()));
      Optional<GraphNodeOptions> nodeC = nodeB.get().get(SUCCESS_TRANSITION);
      assertTrue(nodeC.isPresent());
      assertEquals("c", getAction(nodeC.get()));
    }, vertx);
  }

  @Test
  @DisplayName("Expect 'on' to alias 'onTransitions'")
  void expectAssignedTransitions(Vertx vertx) throws Throwable {
    verify("conf/taskWithTransitions-alias.conf", config -> {
      GraphNodeOptions graphNodeOptions = new GraphNodeOptions(config);
      Map<String, GraphNodeOptions> transitions = graphNodeOptions.getOnTransitions();

      assertFalse(transitions.isEmpty());
    }, vertx);
  }

  @Test
  @DisplayName("Expect 'onTransitions' to take precedence over 'on' alias")
  void expectAliasToTakePrecedence(Vertx vertx) throws Throwable {
    verify("conf/taskWithTransitionsAndAlias.conf", config -> {
      GraphNodeOptions graphNodeOptions = new GraphNodeOptions(config);
      Map<String, GraphNodeOptions> transitions = graphNodeOptions.getOnTransitions();

      assertEquals(transitions.size(), 1);
      assertNotNull(transitions.get("_error"));
    }, vertx);
  }

  private Consumer<JsonObject> validateActionNode() {
    return config -> {
      GraphNodeOptions graphNodeOptions = new GraphNodeOptions(config);
      assertEquals("a", getAction(graphNodeOptions));
      assertEquals(ActionNodeFactory.NAME, graphNodeOptions.getNode().getFactory());
    };
  }

  private Consumer<JsonObject> validateSubtasksNode() {
    return config -> {
      GraphNodeOptions graphNodeOptions = new GraphNodeOptions(config);
      assertEquals(SubtasksNodeFactory.NAME, graphNodeOptions.getNode().getFactory());
      SubtasksNodeConfig nodeConfig = new SubtasksNodeConfig(
          graphNodeOptions.getNode().getConfig());
      assertEquals(2, nodeConfig.getSubtasks().size());
    };
  }

  private String getAction(GraphNodeOptions graphNodeOptions) {
    return new ActionNodeConfig(
        graphNodeOptions.getNode().getConfig()).getAction();
  }
}