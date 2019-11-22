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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.task.exception.NodeConfigException;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.Map;
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
    JsonObject config = new JsonObject();

    // when, then
    Assertions.assertThrows(
        NodeConfigException.class, () -> new ActionNodeFactory().configure(config, vertx));
  }

  @Test
  @DisplayName("Expect graph with single action node without transitions.")
  void expectSingleActionNodeGraph(Vertx vertx) {
    // given
    JsonObject nodeConfig = createNodeConfig("A", SUCCESS_TRANSITION);
    GraphNodeOptions graph = new GraphNodeOptions("A", NO_TRANSITIONS);

    ActionNodeFactory nodeFactory = new ActionNodeFactory();
    nodeFactory.configure(nodeConfig, vertx);

    nodeFactory.initNode(graph, Collections.emptyMap(), null);

  }

  private JsonObject createNodeConfig(String actionName, String transition) {
    return new ActionNodeFactoryConfig(Collections.singletonMap(actionName,
        new ActionOptions(new JsonObject())
            .setFactory("test-action")
            .setConfig(new JsonObject().put("transition", transition))))
        .toJson();
  }

}