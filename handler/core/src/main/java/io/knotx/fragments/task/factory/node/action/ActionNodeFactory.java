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

import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.single.SingleNode;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.engine.api.node.single.FragmentContext;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.knotx.fragments.task.factory.ActionFactoryOptions;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.NodeFactory;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActionNodeFactory implements NodeFactory {

  public static final String NAME = "action";
  private ActionProvider actionProvider;
  private Map<String, ActionFactoryOptions> actionNameToOptions;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public ActionNodeFactory configure(JsonObject config, Vertx vertx) {
    this.actionNameToOptions = new ActionNodeFactoryConfig(config).getActions();
    actionProvider = new ActionProvider(supplyFactories(), actionNameToOptions, vertx);
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
    ActionNodeConfig config = new ActionNodeConfig(nodeOptions.getNode().getConfig());
    Action action = actionProvider.get(config.getAction()).orElseThrow(
        () -> new ActionNotFoundException(config.getAction()));
    final String actionNodeId = UUID.randomUUID().toString();
    nodesMetadata.put(actionNodeId, createActionNodeMetadata(actionNodeId, edges, config));
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
      public Single<FragmentResult> execute(FragmentContext fragmentContext) {
        return toRxFunction(action).apply(fragmentContext);
      }
    };
  }

  private NodeMetadata createActionNodeMetadata(String actionNodeId, Map<String, Node> edges,
      ActionNodeConfig config) {
    Map<String, String> transitionMetadata = createTransitionMetadata(edges);
    JsonObject configWithActionConfiguration = createJointConfig(config);
    return new NodeMetadata(actionNodeId, NodeType.SINGLE, transitionMetadata, new ArrayList<>(),
        configWithActionConfiguration);
  }

  private Map<String, String> createTransitionMetadata(Map<String, Node> edges) {
    Map<String, String> transitionMetadata = new HashMap<>();
    edges.forEach((transition, node) -> transitionMetadata.put(transition, node.getId()));
    return transitionMetadata;
  }

  private JsonObject createJointConfig(ActionNodeConfig config) {
    ActionFactoryOptions actionConfig = actionNameToOptions.get(config.getAction());
    return new JsonObject()
        .put("type", NAME)
        .put("alias", config.getAction())
        .put("factory", actionConfig.getFactory())
        .put("actionConfig", actionConfig.toJson());
  }

  private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
      Action action) {
    io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
        .newInstance(action);
    return rxAction::rxApply;
  }

  private Supplier<Iterator<ActionFactory>> supplyFactories() {
    return () -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader
          .load(ActionFactory.class);
      return factories.iterator();
    };
  }
}
