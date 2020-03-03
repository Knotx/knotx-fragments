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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class EventLogConverter {

  private static final String LOG_STATUS = "_logStatus";
  private static final String RAW_LOGS = "_rawLogs";
  private static final String STATUS = "status";

  private final List<EventLogEntry> operationsLog;

  EventLogConverter(List<EventLogEntry> operationsLog) {
    this.operationsLog = operationsLog;
  }

  JsonObject fillWithLog(JsonObject input, String id) {
    List<EventLogEntry> logs = getLogEntriesFor(id);
    if (logs.isEmpty()) {
      return input.put(LOG_STATUS, "MISSING")
          .put(STATUS, NodeStatus.MISSING);
    } else if (logs.size() == 1) {
      EventLogEntry log = logs.get(0);
      return input.put(STATUS, log.getStatus())
          .put("response", new JsonObject()
              .put("transition", log.getTransition())
              .put("invocations", wrap(log.getNodeLog()))
          )
          .put(LOG_STATUS, "ok");
    } else {
      try {
        return handleMultipleLogEntries(input, logs);
      } catch (IllegalArgumentException e) {
        return input
            .put(LOG_STATUS, "multiple")
            .put(RAW_LOGS, wrap(logs));
      }
    }
  }

  private List<EventLogEntry> getLogEntriesFor(String id) {
    if (id != null) {
      return operationsLog.stream().filter(entry -> id.equals(entry.getNode()))
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  private JsonObject handleMultipleLogEntries(JsonObject input, List<EventLogEntry> logs) {
    /*
     * This is a workaround for the case when Fragment's EventLog contains two entries
     * for node with the given id. If these entries turn out to be originating from a finished
     * node's execution followed by a transition that was not supported, then this case is handled.
     */
    if (logs.size() != 2) {
      throw new IllegalArgumentException();
    }

    EventLogEntry executionLog = getLogForExecution(logs);
    EventLogEntry transitionLog = getLogForUnsupportedTransition(logs);

    return input
        .put(STATUS, transitionLog.getStatus())
        .put("response", new JsonObject()
            .put("transition", transitionLog.getTransition())
            .put("invocations", wrap(executionLog.getNodeLog()))
        )
        .put(LOG_STATUS, "ok");
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

  private JsonArray wrap(JsonObject instance) {
    if (instance != null) {
      return new JsonArray().add(instance);
    } else {
      return new JsonArray();
    }
  }

  private JsonArray wrap(List<EventLogEntry> entries) {
    JsonArray output = new JsonArray();
    entries.forEach(entry -> output.add(entry.toJson()));
    return output;
  }

}
