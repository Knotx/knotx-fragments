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

import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SingleNodeWithMetadata extends NodeWithMetadata implements SingleNode {

  private final String id;

  private final Action action;

  private final Map<String, NodeWithMetadata> edges;

  private final String factory;

  public SingleNodeWithMetadata(String id, Action action,
      Map<String, NodeWithMetadata> edges, String factory) {
    this.id = id;
    this.action = action;
    this.edges = edges;
    this.factory = factory;
  }

  @Override
  public Single<FragmentResult> execute(FragmentContext fragmentContext) {
    return toRxFunction(action).apply(fragmentContext)
        .doOnSuccess(fragmentResult -> metadata
            .put("response", new JsonObject()
                .put("transition", fragmentResult.getTransition())
                .put("invocations", fragmentResult.getNodeLog())));
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
  public JsonObject generateMetadata() {
    determineMissingChildren(edges);
    metadata
        .put("id", id)
        .put("type", "single")
        .put("operation", new JsonObject()
            .put("type", "action")
            .put("factory", factory))
        .put("label", "some label")
        .put("on", JsonObject.mapFrom(edges.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().generateMetadata()))));
    determineStatus();
    return metadata;
  }

  private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
      Action action) {
    io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
        .newInstance(action);
    return rxAction::rxApply;
  }
}
