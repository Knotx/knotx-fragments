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
package io.knotx.fragments.handler.consumer;

import io.knotx.fragments.engine.EventLogEntry;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.TaskMetadata;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class GraphDataConverter {

  private final String rootNodeId;
  private final Map<String, NodeMetadata> nodes;
  private final Map<String, EventLogEntry> operationsLog;

  private GraphDataConverter(FragmentEvent event, TaskMetadata taskMetadata) {
    this.rootNodeId = taskMetadata.getRootNodeId();
    this.nodes = taskMetadata.getNodesMetadata();
    this.operationsLog = event.getLog().getOperations().stream()
        .collect(Collectors.toMap(EventLogEntry::getNode, entry -> entry));
  }

  static GraphDataConverter from(FragmentEvent event, TaskMetadata metadata) {
    return new GraphDataConverter(event, metadata);
  }

  JsonObject createJson() {
    return createNodeJson(rootNodeId);
  }

  private JsonObject createNodeJson(String id) {
    // Fallback: no metadata for node of given id
    if (!nodes.containsKey(id)) {
      return Optional.ofNullable(operationsLog.get(id))
          .map(log -> new JsonObject().put("_rawLog", log))
          .orElseGet(JsonObject::new)
          .put("id", id)
          .put("_metadataStatus", "missing");
    }

    NodeMetadata metadata = nodes.get(id);
    EventLogEntry log = operationsLog.get(id);
    // We assume there is only a single log entry for any given nodeID

    return new JsonObject()
        .put("id", metadata.getNodeId())
        .put("type", metadata.getType())
        .put("label", metadata.getNodeId()) // TODO: where label configuration should come from?
        .put("subtasks", getSubTasks(metadata.getNestedNodes()))
        .put("operation", metadata.getOperation()) // TODO: validate if this is expandable
        .put("status", log.getStatus())
        .put("on", createTransitions(id))
        .put("response", new JsonObject()
            .put("transition", log.getTransition())
            .put("invocations", log.getNodeLog())
        );
  }

  private JsonObject createTransitions(String id) {
    JsonObject transitions = new JsonObject();
    nodes.get(id).getTransitions()
        .forEach((name, nextId) -> transitions.put(name, createNodeJson(nextId)));
    return transitions;
  }

  private JsonArray getSubTasks(List<String> nestedNodes) {
    return new JsonArray(
        nestedNodes.stream()
            .map(this::createNodeJson)
            .collect(Collectors.toList())
    );
  }

}
