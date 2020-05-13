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

import static io.knotx.fragments.task.factory.api.metadata.NodeMetadata.single;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.action.core.ActionProvider;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.single.SingleNode;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.knotx.fragments.task.factory.generic.GraphNodeOptions;
import io.knotx.fragments.task.factory.generic.NodeProvider;
import io.knotx.fragments.task.factory.generic.node.NodeFactory;
import io.knotx.fragments.task.factory.generic.node.NodeOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Supplier;

public class ActionNodeFactory implements NodeFactory {

  public static final String NAME = "action";
  public static final String METADATA_ALIAS = "alias";
  public static final String METADATA_ACTION_FACTORY = "actionFactory";
  public static final String METADATA_ACTION_CONFIG = "actionConfig";
  private ActionProvider actionProvider;
  private Map<String, ActionFactoryOptions> actionNameToOptions;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public ActionNodeFactory configure(JsonObject config, Vertx vertx) {
    this.actionNameToOptions = new ActionNodeFactoryConfig(config).getActions();
    Supplier<Iterator<ActionFactory>> actionFactoriesSupplier = () -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader.load(ActionFactory.class);
      return factories.iterator();
    };
    actionProvider = new ActionProvider(actionFactoriesSupplier, actionNameToOptions, vertx);
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
    ActionNodeConfig config = new ActionNodeConfig(nodeOptions.getConfig());
    final String actionNodeId = UUID.randomUUID().toString();

    Action action = actionProvider.get(config.getAction())
        .orElseThrow(() -> new ActionNotFoundException(config.getAction()));

    NodeMetadata metadata = createActionNodeMetadata(actionNodeId, edges, config);
    nodesMetadata.put(actionNodeId, metadata);

    return new SingleNode() {
      @Override
      public String getId() {
        return actionNodeId;
      }

      @Override
      public Optional<Node> next(String transition) {
        return Optional.ofNullable(edges.get(transition));
      }

      @Override
      public void apply(FragmentContext fragmentContext, Handler<AsyncResult<FragmentResult>> handler) {
        action.apply(fragmentContext, handler);
      }
    };
  }

  private NodeMetadata createActionNodeMetadata(String actionNodeId, Map<String, Node> edges,
      ActionNodeConfig config) {
    Map<String, String> transitionMetadata = createTransitionMetadata(edges);
    return single(actionNodeId, config.getAction(), transitionMetadata, createOperation(config));
  }

  private Map<String, String> createTransitionMetadata(Map<String, Node> edges) {
    Map<String, String> transitionMetadata = new HashMap<>();
    edges.forEach((transition, node) -> transitionMetadata.put(transition, node.getId()));
    return transitionMetadata;
  }

  private OperationMetadata createOperation(ActionNodeConfig config) {
    ActionFactoryOptions actionConfig = actionNameToOptions.get(config.getAction());
    return new OperationMetadata(NAME, new JsonObject()
        .put(METADATA_ALIAS, config.getAction())
        .put(METADATA_ACTION_FACTORY, actionConfig.getFactory())
        .put(METADATA_ACTION_CONFIG, actionConfig.getConfig()));
  }
}
