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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.fragments.task.options.SubtasksNodeConfigOptions;
import io.vertx.reactivex.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubtasksNodeFactory implements NodeFactory {

  @Override
  public String getName() {
    return "subtasks";
  }

  public Node newInstance(String taskName, GraphNodeOptions nodeOptions,
      Map<String, Node> edges,
      TaskFactoryOptions taskFactoryOptions, DefaultTaskFactory taskFactory, Vertx vertx) {
    SubtasksNodeConfigOptions config = new SubtasksNodeConfigOptions(
        nodeOptions.getNode().getConfig());
    List<Node> nodes = config.getSubtasks().stream()
        .map((GraphNodeOptions nextNodeOptions) -> taskFactory
            .initNode(taskName, nextNodeOptions, taskFactoryOptions,
                vertx))
        .collect(Collectors.toList());
    return new CompositeNode(getNodeId(), nodes, edges.get(SUCCESS_TRANSITION),
        edges.get(ERROR_TRANSITION));
  }

  private String getNodeId() {
    // TODO this value should be calculated based on graph, the behaviour now is not changed
    return "composite";
  }
}
