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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.engine.graph.CompositeNode;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class CompositeNodeWithMetadata implements CompositeNode, NodeWithMetadata {

  private final String id;

  private final List<NodeWithMetadata> nodes;

  private final Map<String, NodeWithMetadata> edges;

  private final JsonObject metadata = new JsonObject();

  public CompositeNodeWithMetadata(String id, List<NodeWithMetadata> nodes,
      Map<String, NodeWithMetadata> edges) {
    this.id = id;
    this.nodes = nodes;
    this.edges = edges;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Optional<NodeWithMetadata> next(String transition) {
    return filter(transition).map(edges::get);
  }

  @Override
  public List<NodeWithMetadata> getNodes() {
    return nodes;
  }

  @Override
  public JsonObject getData() {
    metadata
        .put("id", id)
        .put("type", "composite")
        .put("subtasks", nodes.stream().map(NodeWithMetadata::getData).collect(Collectors.toList()))
        .put("label", "some label")
        .put("on", JsonObject.mapFrom(edges.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getData()))));
    return metadata;
  }

  private Optional<String> filter(String transition) {
    return ERROR_TRANSITION.equals(transition) || SUCCESS_TRANSITION.equals(transition)
          ? Optional.of(transition) : Optional.empty();
  }
}
