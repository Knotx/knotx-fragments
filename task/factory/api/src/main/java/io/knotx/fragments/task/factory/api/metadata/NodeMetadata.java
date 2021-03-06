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
package io.knotx.fragments.task.factory.api.metadata;

import io.knotx.fragments.task.api.NodeType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NodeMetadata {

  private final String nodeId;
  private final String label;
  private final NodeType type;
  private final Map<String, String> transitions;
  private final List<String> nestedNodes;
  private final OperationMetadata operation;

  public static NodeMetadata single(String nodeId, String label, Map<String, String> transitions,
      OperationMetadata operation) {
    return new NodeMetadata(nodeId, label, NodeType.SINGLE, transitions, Collections.emptyList(),
        operation);
  }

  public static NodeMetadata composite(String nodeId, String label, Map<String, String> transitions,
      List<String> nestedNodes, OperationMetadata operation) {
    return new NodeMetadata(nodeId, label, NodeType.COMPOSITE, transitions, nestedNodes, operation);
  }

  private NodeMetadata(String nodeId, String label, NodeType type, Map<String, String> transitions,
      List<String> nestedNodes, OperationMetadata operation) {
    this.nodeId = nodeId;
    this.label = label;
    this.type = type;
    this.transitions = transitions;
    this.nestedNodes = nestedNodes;
    this.operation = operation;
  }

  public String getNodeId() {
    return nodeId;
  }

  public String getLabel() {
    return label;
  }

  public NodeType getType() {
    return type;
  }

  /**
   * @return transition name to node id map
   */
  public Map<String, String> getTransitions() {
    return transitions;
  }

  /**
   * @return list of composite nodes identifiers
   */
  public List<String> getNestedNodes() {
    return nestedNodes;
  }

  public OperationMetadata getOperation() {
    return operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodeMetadata that = (NodeMetadata) o;
    return Objects.equals(nodeId, that.nodeId) &&
        Objects.equals(label, that.label) &&
        type == that.type &&
        Objects.equals(transitions, that.transitions) &&
        Objects.equals(nestedNodes, that.nestedNodes) &&
        Objects.equals(operation, that.operation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, label, type, transitions, nestedNodes, operation);
  }

  @Override
  public String toString() {
    return "NodeMetadata{" +
        "nodeId='" + nodeId + '\'' +
        ", label='" + label + '\'' +
        ", type=" + type +
        ", transitions=" + transitions +
        ", nestedNodes=" + nestedNodes +
        ", operation=" + operation +
        '}';
  }
}
