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
package io.knotx.fragments.handler.consumer.metadata;

import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.TaskMetadata;
import io.knotx.fragments.engine.api.node.NodeType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class MetadataConverter {

  private static final String METADATA_STATUS = "_metadataStatus";
  private static final String MISSING = "missing";
  private static final String LOG_STATUS = "_logStatus";

  private final String rootNodeId;
  private final Map<String, NodeMetadata> nodes;
  private final EventLogConverter eventLogConverter;

  private MetadataConverter(FragmentEvent event, TaskMetadata taskMetadata) {
    this.rootNodeId = taskMetadata.getRootNodeId();
    this.nodes = taskMetadata.getNodesMetadata();
    this.eventLogConverter = new EventLogConverter(event.getLog().getOperations());
  }

  public static MetadataConverter from(FragmentEvent event, TaskMetadata metadata) {
    return new MetadataConverter(event, metadata);
  }

  public JsonObject createJson() {
    if (StringUtils.isBlank(rootNodeId)) {
      return new JsonObject();
    } else {
      return createNodeJson(rootNodeId);
    }
  }

  private JsonObject createNodeJson(String id) {
    return Optional.of(new JsonObject())
        .map(json -> fillWithMetadata(json, id))
        .map(json -> eventLogConverter.fillWithLog(json, id))
        .map(this::fillMissingNode)
        .orElseGet(JsonObject::new);  // Should never happen
  }

  private JsonObject fillMissingNode(JsonObject input) {
    Optional.of(input)
        .map(metadata -> metadata.getJsonObject("response"))
        .map(response -> response.getString("transition"))
        .filter(transition -> !"_success".equals(transition))
        .ifPresent(transition -> Optional.of(input)
            .map(metadata -> metadata.getJsonObject("on"))
            .filter(transitions -> !transitions.containsKey(transition))
            .ifPresent(transitions -> transitions.put(transition, missingNodeData())));
    return input;
  }

  private JsonObject missingNodeData() {
    return new JsonObject()
        .put("id", UUID.randomUUID().toString())
        .put("label", "!")
        .put("type", NodeType.SINGLE)
        .put("status", MISSING)
        .put(METADATA_STATUS, MISSING)
        .put(LOG_STATUS, MISSING);
  }

  private JsonObject fillWithMetadata(JsonObject input, String id) {
    if (nodes.containsKey(id)) {
      NodeMetadata metadata = nodes.get(id);
      return input.put("id", metadata.getNodeId())
          .put("type", metadata.getType())
          .put("label", metadata.getNodeId()) // TODO: where should label configuration come from?
          .put("subtasks", getSubTasks(metadata.getNestedNodes()))
          .put("operation", metadata.getOperation().toJson()) // TODO: validate if this is expandable
          .put("on", getTransitions(metadata.getTransitions()))
          .put(METADATA_STATUS, "ok");
    } else {
      return input.put("id", id)
          .put(METADATA_STATUS, MISSING);
    }
  }

  private JsonObject getTransitions(Map<String, String> definedTransitions) {
    JsonObject output = new JsonObject();
    definedTransitions.forEach((name, nextId) -> output.put(name, createNodeJson(nextId)));
    return output;
  }

  private JsonArray getSubTasks(List<String> nestedNodes) {
    JsonArray output = new JsonArray();
    nestedNodes.stream()
        .map(this::createNodeJson)
        .forEach(output::add);
    return output;
  }

}
