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

import io.knotx.fragments.handler.api.metadata.NodeMetadata;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.task.factory.node.NodeOptions;
import java.util.Map;

/**
 * Initialize {@link io.knotx.fragments.engine.api.node.single.SingleNode} or {@link
 * io.knotx.fragments.engine.api.node.composite.CompositeNode} instances based on node options.
 */
public interface NodeProvider {

  /**
   * Init a graph node based on provided options. If factory defined in {@link
   * NodeOptions#getFactory()} is not found, then it throws {@link io.knotx.fragments.task.factory.exception.NodeFactoryNotFoundException}.
   *
   * @param nodeOptions node options
   * @return {@link io.knotx.fragments.engine.api.node.single.SingleNode} or {@link
   * io.knotx.fragments.engine.api.node.composite.CompositeNode} instance
   * @deprecated use {@link #initNode(GraphNodeOptions, Map)} instead
   */
  @Deprecated
  Node initNode(GraphNodeOptions nodeOptions);

  /**
   * Init a graph node based on provided options. If factory defined in {@link
   * NodeOptions#getFactory()} is not found, then it throws {@link io.knotx.fragments.task.factory.exception.NodeFactoryNotFoundException}.
   * Additionally it adds node's metadata to nodesMetadata map.
   *
   * @param nodeOptions node options
   * @return {@link io.knotx.fragments.engine.api.node.single.SingleNode} or {@link
   * io.knotx.fragments.engine.api.node.composite.CompositeNode} instance
   */
  default Node initNode(GraphNodeOptions nodeOptions, Map<String, NodeMetadata> nodesMetadata) {
    return initNode(nodeOptions);
  }

}
