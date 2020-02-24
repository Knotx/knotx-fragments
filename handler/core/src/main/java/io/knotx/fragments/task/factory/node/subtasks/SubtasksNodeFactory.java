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

import static io.knotx.fragments.engine.api.node.single.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.composite.CompositeNode;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.NodeFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class SubtasksNodeFactory implements NodeFactory {

  public static final String NAME = "subtasks";

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
  public Node initNode(GraphNodeOptions nodeOptions, Map<String, Node> edges,
      NodeProvider nodeProvider) {
    // The implementation is for backwards compatibility of NodeFactory interface
    return initNode(nodeOptions, edges, nodeProvider, new HashMap<>());
  }

  @Override
  public Node initNode(GraphNodeOptions nodeOptions, Map<String, Node> edges,
      NodeProvider nodeProvider, Map<String, NodeMetadata> nodesMetadata) {
    SubtasksNodeConfig config = new SubtasksNodeConfig(nodeOptions.getNode().getConfig());
    List<Node> nodes = config.getSubtasks().stream()
        .map(subTaskConfig -> nodeProvider.initNode(subTaskConfig, nodesMetadata))
        .collect(Collectors.toList());
    final String nodeId = UUID.randomUUID().toString();
    nodesMetadata.put(nodeId, createSubTaskNodeMetadata(nodeId, edges, nodes));
    return new CompositeNode() {
      @Override
      public String getId() {
        return nodeId;
      }

      @Override
      public Optional<Node> next(String transition) {
        return filter(transition).map(edges::get);
      }

      @Override
      public List<Node> getNodes() {
        return nodes;
      }

      private Optional<String> filter(String transition) {
        return Optional.of(transition)
            .filter(tr -> StringUtils.equalsAny(tr, ERROR_TRANSITION, SUCCESS_TRANSITION));
      }
    };
  }

  private NodeMetadata createSubTaskNodeMetadata(String nodeId, Map<String, Node> edges,
      List<Node> nodes) {
    List<String> nestedNodesIds = nodes.stream().map(Node::getId).collect(Collectors.toList());
    Map<String, String> transitionMetadata = createTransitionMetadata(edges);
    return new NodeMetadata(nodeId, NAME, NodeType.COMPOSITE, transitionMetadata,
        nestedNodesIds, new JsonObject());
  }

  private Map<String, String> createTransitionMetadata(Map<String, Node> edges) {
    Map<String, String> transitionMetadata = new HashMap<>();
    edges.forEach((transition, node) -> transitionMetadata.put(transition, node.getId()));
    return transitionMetadata;
  }

}
