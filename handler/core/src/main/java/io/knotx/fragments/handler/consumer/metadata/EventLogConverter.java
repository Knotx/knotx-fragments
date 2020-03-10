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

import io.knotx.fragments.engine.EventLogEntry;
import io.knotx.fragments.engine.EventLogEntry.NodeStatus;
import io.knotx.fragments.handler.LoggedNodeStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class EventLogConverter {

  private final List<EventLogEntry> operationsLog;

  EventLogConverter(List<EventLogEntry> operationsLog) {
    this.operationsLog = operationsLog;
  }

  NodeExecutionData getExecutionData(String id) {
    List<EventLogEntry> logs = getLogEntriesFor(id);
    String transition = getTransition(logs);

    NodeExecutionData result = new NodeExecutionData(getLoggedNodesStatus(logs));
    if (transition != null) {
      result.setResponse(transition, inJsonArray(getNodeLog(logs)));
    }
    return result;
  }

  private LoggedNodeStatus getLoggedNodesStatus(List<EventLogEntry> logs) {
    return getLogForExecution(logs)
        .map(LoggedNodeStatus::from)
        .orElse(LoggedNodeStatus.UNPROCESSED);
  }

  private String getTransition(List<EventLogEntry> logs) {
    return getLogForExecution(logs)
        .map(EventLogEntry::getTransition)
        .orElse(null);
  }

  private JsonObject getNodeLog(List<EventLogEntry> logs) {
    return getLogForExecution(logs)
        .map(EventLogEntry::getNodeLog)
        .orElse(null);
  }

  private List<EventLogEntry> getLogEntriesFor(String id) {
    return operationsLog.stream()
        .filter(entry -> StringUtils.equals(id, entry.getNode()))
        .collect(Collectors.toList());
  }

  private Optional<EventLogEntry> getLogForExecution(List<EventLogEntry> logs) {
    return logs.stream()
        .filter(log -> !NodeStatus.UNSUPPORTED_TRANSITION.equals(log.getStatus()))
        .findFirst();
  }

  private JsonArray inJsonArray(JsonObject instance) {
    if (instance != null) {
      return new JsonArray().add(instance);
    } else {
      return new JsonArray();
    }
  }

}
