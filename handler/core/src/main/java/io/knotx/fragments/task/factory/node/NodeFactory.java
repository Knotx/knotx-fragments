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
package io.knotx.fragments.task.factory.node;

import static java.util.Collections.emptyMap;

import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.NodeProvider;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.Map;

/**
 * A node factory interface allowing to register a node factory by its name. Implementing class must
 * be configured in <code>META-INF.services</code>.
 *
 * Node factories are configured in {@link io.knotx.fragments.task.factory.DefaultTaskFactory#configure(JsonObject,
 * Vertx)}.
 */
public interface NodeFactory {

  /**
   * @return node factory name
   */
  String getName();

  /**
   * Configures a node factory with config defined in {@link NodeFactoryOptions#getConfig()}. This
   * method is called during factories initialization.
   *
   * @param config - json node factory configuration, see {@link NodeFactoryOptions#getConfig()}
   * @param vertx - vertx instance
   * @return a reference to this, so the API can be used fluently
   * @see NodeFactoryOptions#getConfig()
   */
  NodeFactory configure(JsonObject config, Vertx vertx);

  /**
   * Initialize node instance. Nodes are stateless and stateful.
   *
   * @param nodeOptions - graph node options
   * @param edges - prepared node outgoing edges
   * @param nodeProvider - node provider if the current node contains others
   * @return node instance
   * @deprecated use {@link #initNode(NodeOptions, Map, NodeProvider, Map)} instead.
   */
  @Deprecated
  Node initNode(GraphNodeOptions nodeOptions, Map<String, Node> edges, NodeProvider nodeProvider);

  /**
   * Initialize node instance. Nodes are stateless and stateful.
   *
   * @param nodeOptions - graph node options
   * @param edges - prepared node outgoing edges
   * @param nodeProvider - node provider if the current node contains others
   * @param nodesMetadata - node id to metadata map
   * @return node instance
   */
  default Node initNode(NodeOptions nodeOptions, Map<String, Node> edges, NodeProvider nodeProvider,
      Map<String, NodeMetadata> nodesMetadata) {
    return initNode(new GraphNodeOptions(nodeOptions, emptyMap()), edges, nodeProvider);
  }
}
