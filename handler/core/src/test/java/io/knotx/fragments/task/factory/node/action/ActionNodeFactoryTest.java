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

import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.action.ActionFactoryOptions;
import io.knotx.fragments.task.factory.node.StubNode;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
            .initNode(graph, Collections.emptyMap(), null));
  }

  @Test
  @DisplayName("Expect exception when action not found.")
  void expectExceptionWhenActionNotFound(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig("otherAction", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when, then
    Assertions.assertThrows(
        ActionNotFoundException.class, () -> new ActionNodeFactory().configure(config, vertx)
            .initNode(graph, Collections.emptyMap(), null));
  }

  @Test
  @DisplayName("Expect A node when action node defined.")
  void expectSingleActionNode(Vertx vertx) {
    // given
    String actionAlias = "A";
    JsonObject config = createNodeConfig(actionAlias, SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, NO_TRANSITIONS);

    // when
    Node node = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, Collections.emptyMap(), null);

    // then
    assertEquals(actionAlias, node.getId());
    assertTrue(node instanceof SingleNode);
  }

  @Test
  @DisplayName("Expect node contains passed transitions.")
  void expectActionNodesGraphWithTransition(Vertx vertx) {
    // given
    String actionAlias = "A";
    String transition = "transition";
    JsonObject config = createNodeConfig(actionAlias, SUCCESS_TRANSITION);
    // this invalid configuration is expected
    GraphNodeOptions graph = new GraphNodeOptions(actionAlias, Collections.emptyMap());

    // when
    Node node = new ActionNodeFactory().configure(config, vertx)
        .initNode(graph, Collections.singletonMap(transition, new StubNode("B")), null);

    // then
    Optional<Node> nextNode = node.next(transition);
    assertTrue(nextNode.isPresent());
    assertEquals("B", nextNode.get().getId());
  }

  private JsonObject createNodeConfig(String actionName, String transition) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionFactoryOptions(new JsonObject())
            .setFactory("test-action")
            .setConfig(new JsonObject().put("transition", transition))))
        .toJson();
  }
}