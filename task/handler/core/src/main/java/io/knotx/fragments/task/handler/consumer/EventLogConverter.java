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
package io.knotx.fragments.task.handler.consumer;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.task.engine.EventLogEntry;
import io.knotx.fragments.task.engine.EventLogEntry.NodeStatus;
import io.knotx.fragments.task.handler.log.api.model.LoggedNodeStatus;
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

    result.setStarted(getStartTimestamp(logs));
    result.setFinished(getFinishTimestamp(logs));

    return result;
  }

  private long getStartTimestamp(List<EventLogEntry> logs) {
    return getLogForStart(logs)
        .map(EventLogEntry::getTimestamp)
        .orElse(0L);
  }

  private long getFinishTimestamp(List<EventLogEntry> logs) {
    return getLogForExecution(logs)
        .map(EventLogEntry::getTimestamp)
        .orElse(0L);
  }

  private LoggedNodeStatus getLoggedNodesStatus(List<EventLogEntry> logs) {
    return getLogForExecution(logs)
        .map(this::toNodeStatus)
        .orElse(LoggedNodeStatus.UNPROCESSED);
  }

  protected LoggedNodeStatus toNodeStatus(EventLogEntry logEntry) {
    String transition = logEntry.getTransition();
    NodeStatus status = logEntry.getStatus();

    final LoggedNodeStatus result;
    if (SUCCESS_TRANSITION.equals(transition)) {
      result = LoggedNodeStatus.SUCCESS;
    } else if (ERROR_TRANSITION.equals(transition) || status == NodeStatus.TIMEOUT) {
      result = LoggedNodeStatus.ERROR;
    } else if (StringUtils.isNotEmpty(transition) && status != NodeStatus.UNSUPPORTED_TRANSITION) {
      result = LoggedNodeStatus.OTHER;
    } else {
      result = LoggedNodeStatus.UNPROCESSED;
    }
    return result;
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
        .filter(this::hasCorrectTransition)
        .reduce((previous, current) -> current);
  }

  private Optional<EventLogEntry> getLogForStart(List<EventLogEntry> logs) {
    return logs.stream()
        .filter(this::hasCorrectTransition)
        .findFirst();
  }

  private boolean hasCorrectTransition(EventLogEntry log) {
    return !NodeStatus.UNSUPPORTED_TRANSITION.equals(log.getStatus());
  }

  private JsonArray inJsonArray(JsonObject instance) {
    if (instance != null) {
      return new JsonArray().add(instance);
    } else {
      return new JsonArray();
    }
  }

}
