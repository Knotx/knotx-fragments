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
package io.knotx.fragments.task.engine;

import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

public class EventLogEntry {

  public enum NodeStatus {
    SUCCESS,
    UNSUPPORTED_TRANSITION,
    ERROR,
    UNPROCESSED
  }

  private final String task;
  private final String node;
  private final NodeStatus status;
  private final String transition;
  private final long timestamp;
  private final JsonObject nodeLog;
  private final Throwable error;

  EventLogEntry(String task, String node, NodeStatus status, String transition,
      JsonObject nodeLog, Throwable error) {
    this.task = task;
    this.node = node;
    this.status = status;
    this.transition = transition;
    this.timestamp = System.currentTimeMillis();
    this.nodeLog = nodeLog == null ? new JsonObject() : nodeLog;
    this.error = error;
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

  public Throwable getError() {
    return error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventLogEntry that = (EventLogEntry) o;
    return timestamp == that.timestamp &&
        Objects.equals(task, that.task) &&
        Objects.equals(node, that.node) &&
        status == that.status &&
        Objects.equals(transition, that.transition) &&
        Objects.equals(nodeLog, that.nodeLog) &&
        Objects.equals(error, that.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(task, node, status, transition, timestamp, nodeLog, error);
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
        ", error=" + error +
        '}';
  }
}
