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
package io.knotx.fragments.task.factory.node.subtasks;

import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.CompositeNodeWithMetadata;
import io.knotx.fragments.task.factory.node.NodeFactory;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubtasksNodeFactory implements NodeFactory {

  public static final String NAME = "subtasks";
  public static final String COMPOSITE_NODE_ID = "composite";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public SubtasksNodeFactory configure(JsonObject config, Vertx vertx) {
    // empty
    return this;
  }

  @Override
  public NodeWithMetadata initNode(GraphNodeOptions nodeOptions, Map<String, NodeWithMetadata> edges,
      NodeProvider nodeProvider) {
    SubtasksNodeConfig config = new SubtasksNodeConfig(nodeOptions.getNode().getConfig());
    List<NodeWithMetadata> nodes = config.getSubtasks().stream()
        .map(nodeProvider::initNode)
        .collect(Collectors.toList());
    return new CompositeNodeWithMetadata(getNodeId(), nodes, edges);
  }

  private String getNodeId() {
    // TODO https://github.com/Knotx/knotx-fragments/issues/54
    return COMPOSITE_NODE_ID;
  }
}
