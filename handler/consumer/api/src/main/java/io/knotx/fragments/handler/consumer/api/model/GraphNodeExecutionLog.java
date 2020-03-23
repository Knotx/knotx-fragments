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
package io.knotx.fragments.handler.consumer.api.model;

import io.knotx.fragments.engine.api.node.NodeType;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * It represents a root node data. It contains details about the node factory and node operation
 * result. A root node points to the next ones, so it allows traversing the whole graph. This data
 * can be used to visualize the graph processing flow.
 *
 * @see io.knotx.fragments.engine.api.Task
 * @see io.knotx.fragments.engine.api.node.Node
 * @see io.knotx.fragments.engine.api.node.single.SingleNode
 * @see io.knotx.fragments.engine.api.node.composite.CompositeNode
 * @see io.knotx.fragments.handler.api.metadata.TaskMetadata
 */
@DataObject(generateConverter = true)
public class GraphNodeExecutionLog {

  private String id;
  private NodeType type = NodeType.SINGLE;
  private String label = StringUtils.EMPTY;
  private long started;
  private long finished;
  private List<GraphNodeExecutionLog> subtasks = new ArrayList<>();
  private GraphNodeOperationLog operation = GraphNodeOperationLog.empty();
  private Map<String, GraphNodeExecutionLog> on = new HashMap<>();

  private LoggedNodeStatus status = LoggedNodeStatus.UNPROCESSED;
  private GraphNodeResponseLog response = new GraphNodeResponseLog();


  public static GraphNodeExecutionLog newInstance(String id) {
    return new GraphNodeExecutionLog().setId(id);
  }

  public static GraphNodeExecutionLog newInstance(String id, NodeType type, String label, long started, long finished,
      List<GraphNodeExecutionLog> subtasks, GraphNodeOperationLog operation,
      Map<String, GraphNodeExecutionLog> on) {
    return new GraphNodeExecutionLog()
        .setId(id)
        .setType(type)
        .setLabel(label)
        .setStarted(started)
        .setFinished(finished)
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

  /**
   * Unique node id. In most cases, it is randomly generated value and can differ in subsequent
   * requests.
   *
   * @return unique node identifier
   */
  public String getId() {
    return id;
  }

  public GraphNodeExecutionLog setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Node type value: <code>SINGLE</code> or <code>COMPOSITE</code> .
   *
   * @return node type value
   * @see io.knotx.fragments.engine.api.node.composite.CompositeNode
   * @see io.knotx.fragments.engine.api.node.single.SingleNode
   */
  public NodeType getType() {
    return type;
  }

  public GraphNodeExecutionLog setType(NodeType type) {
    this.type = type;
    return this;
  }

  /**
   * Node label.
   *
   * @return node label
   */
  public String getLabel() {
    return label;
  }

  public GraphNodeExecutionLog setLabel(String label) {
    this.label = label;
    return this;
  }

  /**
   * Processing start timestamp
   *
   * @return start timestamp
   */
  public long getStarted() {
    return started;
  }

  public GraphNodeExecutionLog setStarted(long started) {
    this.started = started;
    return this;
  }

  /**
   * Processing end timestamp
   *
   * @return end timestamp
   */
  public long getFinished() {
    return finished;
  }

  public GraphNodeExecutionLog setFinished(long finished) {
    this.finished = finished;
    return this;
  }

  /**
   * List of composite node subgraphs.  Each item on the list represents the subgraph root node.  It
   * is valid only when @type is <code>COMPOSITE</code>.
   *
   * @return list of root subgraph nodes
   */
  public List<GraphNodeExecutionLog> getSubtasks() {
    return subtasks;
  }

  public GraphNodeExecutionLog setSubtasks(
      List<GraphNodeExecutionLog> subtasks) {
    this.subtasks = subtasks;
    return this;
  }

  /**
   * Node metadata details. This value is initialized based on
   * <code>io.knotx.fragments.handler.api.metadata.NodeMetadata#getOperation()</code> data.
   *
   * @return operation metadata
   */
  public GraphNodeOperationLog getOperation() {
    return operation;
  }

  public GraphNodeExecutionLog setOperation(
      GraphNodeOperationLog operation) {
    this.operation = operation;
    return this;
  }

  /**
   * Map with a <a href="https://github.com/Knotx/knotx-fragments/tree/master/engine#transition">transition</a>
   * key and consequent <a href="https://github.com/Knotx/knotx-fragments/tree/master/engine#node">node</a>
   * as a value.
   */
  public Map<String, GraphNodeExecutionLog> getOn() {
    return on;
  }

  public GraphNodeExecutionLog setOn(Map<String, GraphNodeExecutionLog> on) {
    this.on = on;
    return this;
  }

  /**
   * Node status that is calculated based on node response. See <a href="https://github.com/Knotx/knotx-fragments/blob/feature/html-consumer-docuemntation-update/handler/consumer/html/src/main/java/io/knotx/fragments/handler/consumer/html/model/LoggedNodeStatus.java">LoggedNodeStatus</a>
   * for more details.
   */
  public LoggedNodeStatus getStatus() {
    return status;
  }

  public GraphNodeExecutionLog setStatus(LoggedNodeStatus status) {
    this.status = status;
    return this;
  }

  /**
   * It represents node execution data. It contains response data such as a transition and list of
   * invocations.
   *
   * @return node execution data
   */
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
