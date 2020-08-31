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
package io.knotx.fragments.task.factory.generic.node.action;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.action.core.ActionProvider;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.generic.GraphNodeOptions;
import io.knotx.fragments.task.factory.generic.NodeProvider;
import io.knotx.fragments.task.factory.generic.node.NodeFactory;
import io.knotx.fragments.task.factory.generic.node.NodeOptions;
import io.knotx.fragments.task.factory.generic.node.action.metadata.ActionNodeMetadataProvider;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Supplier;

public class ActionNodeFactory implements NodeFactory {

  private static final Supplier<Iterator<ActionFactory>> SPI_ACTION_SUPPLIER = () -> ServiceLoader
      .load(ActionFactory.class).iterator();

  public static final String NAME = "action";

  private ActionProvider actionProvider;
  private ActionNodeMetadataProvider actionNodeMetadataProvider;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public ActionNodeFactory configure(JsonObject config, Vertx vertx) {
    Map<String, ActionFactoryOptions> actionNameToOptions = new ActionNodeFactoryConfig(config)
        .getActions();
    this.actionNodeMetadataProvider = ActionNodeMetadataProvider.create(NAME, actionNameToOptions);
    this.actionProvider = new ActionProvider(SPI_ACTION_SUPPLIER, actionNameToOptions, vertx);
    return this;
  }

  @Override
  public Node initNode(GraphNodeOptions nodeOptions, Map<String, Node> edges,
      NodeProvider nodeProvider) {
    // The implementation is for backwards compatibility of NodeFactory interface
    return initNode(nodeOptions.getNode(), edges, nodeProvider, new HashMap<>());
  }

  @Override
  public Node initNode(NodeOptions nodeOptions, Map<String, Node> edges, NodeProvider nodeProvider,
      Map<String, NodeMetadata> nodesMetadata) {
    String alias = new ActionNodeConfig(nodeOptions.getConfig()).getAction();
    final String nodeId = UUID.randomUUID().toString();

    Action action = actionProvider.get(alias)
        .orElseThrow(() -> new ActionNotFoundException(alias));

    NodeMetadata metadata = actionNodeMetadataProvider.provideFor(nodeId, edges, alias);
    nodesMetadata.put(nodeId, metadata);

    return new ActionNode(nodeId, edges, action);
  }
}