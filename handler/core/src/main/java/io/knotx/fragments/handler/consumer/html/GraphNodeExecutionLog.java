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
package io.knotx.fragments.handler.consumer.html;

import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.handler.LoggedNodeStatus;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class GraphNodeExecutionLog {

  private String id;
  private NodeType type = NodeType.SINGLE;
  private String label = StringUtils.EMPTY;
  private List<GraphNodeExecutionLog> subtasks = new ArrayList<>();
  private GraphNodeOperationLog operation = GraphNodeOperationLog.empty();
  private Map<String, GraphNodeExecutionLog> on = new HashMap<>();

  private LoggedNodeStatus status = LoggedNodeStatus.SUCCESS;
  private GraphNodeResponseLog response = new GraphNodeResponseLog();


  public static GraphNodeExecutionLog newInstance(String id) {
    return new GraphNodeExecutionLog().setId(id);
  }

  public static GraphNodeExecutionLog newInstance(String id, NodeType type, String label,
      List<GraphNodeExecutionLog> subtasks, GraphNodeOperationLog operation,
      Map<String, GraphNodeExecutionLog> on) {
    return new GraphNodeExecutionLog()
        .setId(id)
        .setType(type)
        .setLabel(label)
        .setSubtasks(subtasks)
        .setOperation(operation)
        .setOn(on)
        .setResponse(new GraphNodeResponseLog());
  }

  public GraphNodeExecutionLog() {
    // default constructor;
  }

  public GraphNodeExecutionLog(JsonObject jsonObject) {
    GraphNodeExecutionLogConverter.fromJson(jsonObject, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    GraphNodeExecutionLogConverter.toJson(this, result);
    return result;
  }

  public String getId() {
    return id;
  }

  public GraphNodeExecutionLog setId(String id) {
    this.id = id;
    return this;
  }

  public NodeType getType() {
    return type;
  }

  public GraphNodeExecutionLog setType(NodeType type) {
    this.type = type;
    return this;
  }

  public String getLabel() {
    return label;
  }

  public GraphNodeExecutionLog setLabel(String label) {
    this.label = label;
    return this;
  }

  public List<GraphNodeExecutionLog> getSubtasks() {
    return subtasks;
  }

  public GraphNodeExecutionLog setSubtasks(
      List<GraphNodeExecutionLog> subtasks) {
    this.subtasks = subtasks;
    return this;
  }

  public GraphNodeOperationLog getOperation() {
    return operation;
  }

  public GraphNodeExecutionLog setOperation(
      GraphNodeOperationLog operation) {
    this.operation = operation;
    return this;
  }

  public Map<String, GraphNodeExecutionLog> getOn() {
    return on;
  }

  public GraphNodeExecutionLog setOn(Map<String, GraphNodeExecutionLog> on) {
    this.on = on;
    return this;
  }

  public LoggedNodeStatus getStatus() {
    return status;
  }

  public GraphNodeExecutionLog setStatus(LoggedNodeStatus status) {
    this.status = status;
    return this;
  }

  public GraphNodeResponseLog getResponse() {
    return response;
  }

  public GraphNodeExecutionLog setResponse(GraphNodeResponseLog response) {
    this.response = response;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GraphNodeExecutionLog that = (GraphNodeExecutionLog) o;
    return Objects.equals(id, that.id) &&
        type == that.type &&
        Objects.equals(label, that.label) &&
        Objects.equals(subtasks, that.subtasks) &&
        Objects.equals(operation, that.operation) &&
        Objects.equals(on, that.on) &&
        status == that.status &&
        Objects.equals(response, that.response);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, label, subtasks, operation, on, status, response);
  }

  @Override
  public String toString() {
    return "GraphNodeExecutionLog{" +
        "id='" + id + '\'' +
        ", type=" + type +
        ", label='" + label + '\'' +
        ", subtasks=" + subtasks +
        ", operation=" + operation +
        ", on=" + on +
        ", status=" + status +
        ", response=" + response +
        '}';
  }
}
