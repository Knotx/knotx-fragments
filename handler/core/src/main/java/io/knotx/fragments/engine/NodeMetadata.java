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
package io.knotx.fragments.engine;

import io.knotx.fragments.engine.api.node.NodeType;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;

public class NodeMetadata {

  private String nodeId;
  private NodeType type;
  private Map<String, String> transitions;
  private List<String> nestedNodes;
  private JsonObject operation;

  public NodeMetadata(String nodeId, NodeType type, Map<String, String> transitions,
      List<String> nestedNodes, JsonObject operation) {
    this.nodeId = nodeId;
    this.type = type;
    this.transitions = transitions;
    this.nestedNodes = nestedNodes;
    this.operation = operation;
  }

  public String getNodeId() {
    return nodeId;
  }

  public NodeType getType() {
    return type;
  }

  public Map<String, String> getTransitions() {
    return transitions;
  }

  public List<String> getNestedNodes() {
    return nestedNodes;
  }

  public JsonObject getOperation() {
    return operation;
  }

  @Override
  public String toString() {
    return "NodeMetadata{" +
        "nodeId='" + nodeId + '\'' +
        ", type=" + type +
        ", transitionNameToNodeIdMap=" + transitions +
        ", nestedNodes=" + nestedNodes +
        ", config=" + operation +
        '}';
  }
}
