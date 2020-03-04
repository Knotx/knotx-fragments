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
import java.util.stream.Collectors;

class EventLogConverter {

  private static final String STATUS = "status";
  private static final String RESPONSE = "response";
  private static final String TRANSITION = "transition";
  private static final String INVOCATIONS = "invocations";

  private final List<EventLogEntry> operationsLog;

  EventLogConverter(List<EventLogEntry> operationsLog) {
    this.operationsLog = operationsLog;
  }

  JsonObject fillWithLog(JsonObject input, String id) {
    List<EventLogEntry> logs = getLogEntriesFor(id);
    LogData logData = getLogData(logs);

    input.put(STATUS, logData.getStatus());

    if (logData.hasResponse()) {
      input.put(RESPONSE, new JsonObject()
        .put(TRANSITION, logData.getTransition())
        .put(INVOCATIONS, inJsonArray(logData.getNodeLog())));
    }

    return input;
  }

  private LogData getLogData(List<EventLogEntry> logs) {
    if (logs.isEmpty()) {
      return new LogData(LoggedNodeStatus.UNPROCESSED, null, null);
    } else if (logs.size() == 1) {
      EventLogEntry log = logs.get(0);

      return new LogData(LoggedNodeStatus.from(log), log.getTransition(), log.getNodeLog());
    } else {
      EventLogEntry executionLog = getLogForExecution(logs);
      EventLogEntry transitionLog = getLogForUnsupportedTransition(logs);

      return executionLog.getStatus() == NodeStatus.SUCCESS
          ? new LogData(LoggedNodeStatus.from(executionLog), transitionLog.getTransition(), executionLog.getNodeLog())
          : new LogData(LoggedNodeStatus.from(transitionLog), transitionLog.getTransition(), executionLog.getNodeLog());
    }
  }

  private List<EventLogEntry> getLogEntriesFor(String id) {
    return operationsLog.stream()
        .filter(entry -> StringUtils.equals(id, entry.getNode()))
        .collect(Collectors.toList());
  }

  private EventLogEntry getLogForExecution(List<EventLogEntry> logs) {
    return singleOrThrow(
        logs.stream()
            .filter(log -> !NodeStatus.UNSUPPORTED_TRANSITION.equals(log.getStatus()))
            .collect(Collectors.toList())
    );
  }

  private EventLogEntry getLogForUnsupportedTransition(List<EventLogEntry> logs) {
    return singleOrThrow(
        logs.stream()
            .filter(log -> NodeStatus.UNSUPPORTED_TRANSITION.equals(log.getStatus()))
            .filter(log -> log.getNodeLog() == null)
            .collect(Collectors.toList())
    );
  }

  private EventLogEntry singleOrThrow(List<EventLogEntry> logs) {
    if (logs.size() == 1) {
      return logs.get(0);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private JsonArray inJsonArray(JsonObject instance) {
    if (instance != null) {
      return new JsonArray().add(instance);
    } else {
      return new JsonArray();
    }
  }

  private static class LogData {
    private LoggedNodeStatus status;
    private String transition;
    private JsonObject nodeLog;

    public LogData(LoggedNodeStatus status, String transition, JsonObject nodeLog) {
      this.status = status;
      this.transition = transition;
      this.nodeLog = nodeLog;
    }

    public LoggedNodeStatus getStatus() {
      return status;
    }

    public String getTransition() {
      return transition;
    }

    public JsonObject getNodeLog() {
      return nodeLog;
    }

    public boolean hasResponse() {
      return transition != null;
    }
  }
}
