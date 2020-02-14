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

import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.single.SingleNode;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.engine.api.node.single.FragmentContext;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.factory.ActionFactoryOptions;
import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.NodeFactory;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
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
    ActionNodeConfig config = new ActionNodeConfig(nodeOptions.getNode().getConfig());
    Action action = actionProvider.get(config.getAction()).orElseThrow(
        () -> new ActionNotFoundException(config.getAction()));
    return new SingleNode() {
      @Override
      public String getId() {
        return config.getAction();
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

  @Override
  public JsonObject getNodeMetadata(GraphNodeOptions nodeOptions, NodeProvider nodeProvider) {
    ActionNodeConfig config = new ActionNodeConfig(nodeOptions.getNode().getConfig());
    return new JsonObject()
        .put("factory", NAME)
        .put("type", "single")
        .put("config", nodeOptions.getNode().getConfig())
        .put("actionConfig", actionNameToOptions.get(config.getAction()).toJson());
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
