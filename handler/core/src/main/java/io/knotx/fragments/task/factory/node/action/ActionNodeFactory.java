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

import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.factory.NodeProvider;
import io.knotx.fragments.task.factory.node.NodeFactory;
import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
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

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public ActionNodeFactory configure(JsonObject config, Vertx vertx) {
    actionProvider = new ActionProvider(supplyFactories(),
        new ActionNodeFactoryConfig(config).getActions(), vertx);
    return this;
  }

  @Override
  public NodeWithMetadata initNode(GraphNodeOptions nodeOptions, Map<String, NodeWithMetadata> edges,
      NodeProvider nodeProvider) {
    ActionNodeConfig config = new ActionNodeConfig(nodeOptions.getNode().getConfig());
    Action action = actionProvider.get(config.getAction()).orElseThrow(
        () -> new ActionNotFoundException(config.getAction()));
    return new SingleNodeWithMetadata(config.getAction(), action, edges);
  }

  private Supplier<Iterator<ActionFactory>> supplyFactories() {
    return () -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader
          .load(ActionFactory.class);
      return factories.iterator();
    };
  }

  class SingleNodeWithMetadata implements SingleNode, NodeWithMetadata {

    private final String id;
    private final Action action;
    private final Map<String, NodeWithMetadata> edges;

    public SingleNodeWithMetadata(String id, Action action,
        Map<String, NodeWithMetadata> edges) {
      this.id = id;
      this.action = action;
      this.edges = edges;
    }

    @Override
    public Single<FragmentResult> execute(FragmentContext fragmentContext) {
      return toRxFunction(action).apply(fragmentContext);
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public Optional<NodeWithMetadata> next(String transition) {
      return Optional.ofNullable(edges.get(transition));
    }

    @Override
    public JsonObject getData() {
      return new JsonObject().put("afadfsdf","fdsfsdfsd");
    }

    private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
        Action action) {
      io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
          .newInstance(action);
      return rxAction::rxApply;
    }
  }
}
