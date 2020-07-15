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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.task.engine;

import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;

public class EventLogEntry {

  private static final String TASK_KEY = "task";
  private static final String NODE_KEY = "node";
  private static final String STATUS_KEY = "status";
  private static final String TRANSITION_KEY = "transition";
  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String NODE_LOG_KEY = "nodeLog";

  private final String task;
  private final String node;
  private final NodeStatus status;
  private final String transition;
  private final long timestamp;
  private final JsonObject nodeLog;

  public static EventLogEntry started(String task, String node) {
    return new EventLogEntry(task, node, NodeStatus.UNPROCESSED, null, null);
  }

  public static EventLogEntry success(String task, String node, FragmentResult fragmentResult) {
    return new EventLogEntry(task, node, NodeStatus.SUCCESS, fragmentResult.getTransition(), fragmentResult.getLog());
  }

  public static EventLogEntry unsupported(String task, String node, String transition) {
    return new EventLogEntry(task, node, NodeStatus.UNSUPPORTED_TRANSITION, transition,null);
  }

  public static EventLogEntry error(String task, String node, String transition, JsonObject actionLog) {
    return new EventLogEntry(task, node, NodeStatus.ERROR, transition, actionLog);
  }

  public static EventLogEntry error(String task, String node, String transition) {
    return new EventLogEntry(task, node, NodeStatus.ERROR, transition,null);
  }

  public static EventLogEntry timeout(String task, String node) {
    return new EventLogEntry(task, node, NodeStatus.TIMEOUT, null, null);
  }

  private EventLogEntry(String task, String node, NodeStatus status, String transition, JsonObject nodeLog) {
    this.task = task;
    this.node = node;
    this.status = status;
    this.transition = transition;
    this.timestamp = System.currentTimeMillis();
    this.nodeLog = nodeLog;
  }

  EventLogEntry(JsonObject json) {
    this.task = json.getString(TASK_KEY);
    this.node = json.getString(NODE_KEY);
    this.status = NodeStatus.valueOf(json.getString(STATUS_KEY));
    this.transition = json.getString(TRANSITION_KEY);
    this.timestamp = json.getLong(TIMESTAMP_KEY);
    this.nodeLog = json.getJsonObject(NODE_LOG_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(TASK_KEY, task)
        .put(NODE_KEY, node)
        .put(STATUS_KEY, status.name())
        .put(TRANSITION_KEY, transition)
        .put(TIMESTAMP_KEY, timestamp)
        .put(NODE_LOG_KEY, nodeLog);
  }

  @Override
  public String toString() {
    return "EventLogEntry{" +
        "task='" + task + '\'' +
        ", node='" + node + '\'' +
        ", status=" + status +
        ", transition='" + transition + '\'' +
        ", timestamp=" + timestamp +
        ", nodeLog=" + nodeLog +
        '}';
  }

  public String getTask() {
    return task;
  }

  public String getNode() {
    return node;
  }

  public NodeStatus getStatus() {
    return status;
  }

  public String getTransition() {
    return transition;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public JsonObject getNodeLog() {
    return nodeLog;
  }

  public enum NodeStatus {
    SUCCESS,
    UNSUPPORTED_TRANSITION,
    ERROR,
    TIMEOUT,
    UNPROCESSED
  }

}
