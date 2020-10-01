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
import io.knotx.fragments.task.engine.EventLogEntry.NodeStatus;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventLog {

  private final List<EventLogEntry> operations;
  private final String taskName;

  public EventLog(String taskName) {
    this.taskName = taskName;
    operations = new ArrayList<>();
  }

  public EventLog nodeStarted(String node) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.UNPROCESSED, null, null, null));
    return this;
  }

  public EventLog compositeSuccess(String node, String transition) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.SUCCESS, transition, null, null));
    return this;
  }

  public EventLog compositeUnprocessed(String node, String transition) {
    // TODO: Change to NodeStatus.UNPROCESSED when validated contract
    // operations.add(new EventLogEntry(taskName, node, NodeStatus.UNPROCESSED, transition, null, null));
    return compositeError(node, transition);
  }

  public EventLog compositeError(String node, String transition) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.ERROR, transition, null, null));
    return this;
  }

  public EventLog success(String node, FragmentResult fragmentResult) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.SUCCESS, fragmentResult.getTransition(),
        fragmentResult.getLog(), null));
    return this;
  }

  public EventLog unsupported(String node, String transition) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.UNSUPPORTED_TRANSITION, transition, null, null));
    return this;
  }

  public EventLog error(String node, String transition) {
    return error(node, transition, null);
  }

  public EventLog error(String node, FragmentResult fragmentResult) {
    return error(node, fragmentResult.getTransition(), fragmentResult.getLog());
  }

  public EventLog error(String node, String transition, JsonObject nodeLog) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.ERROR, transition, nodeLog, null));
    return this;
  }

  public EventLog exception(String node, String transition, Throwable error) {
    operations.add(new EventLogEntry(taskName, node, NodeStatus.ERROR, transition, null, error));
    return this;
  }

  public void append(EventLogEntry logEntry) {
    operations.add(logEntry);
  }

  public void appendAll(EventLog log) {
    this.operations.addAll(log.operations);
  }

  public List<EventLogEntry> getOperations() {
    return new ArrayList<>(operations);
  }

  public long getEarliestTimestamp() {
    return operations.stream()
        .mapToLong(EventLogEntry::getTimestamp)
        .min()
        .orElse(0);
  }

  public long getLatestTimestamp() {
    return operations.stream()
        .mapToLong(EventLogEntry::getTimestamp)
        .max()
        .orElse(0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventLog eventLog = (EventLog) o;
    return Objects.equals(operations, eventLog.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operations);
  }

  @Override
  public String toString() {
    return "EventLog{" +
        "operations=" + operations +
        '}';
  }

}
