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
package io.knotx.engine.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventLog {

  private static final String OPERATIONS_KEY = "operations";

  private final List<EventLogEntry> operations;

  EventLog() {
    operations = new ArrayList<>();
  }

  EventLog(JsonObject json) {
    operations = json.getJsonArray(OPERATIONS_KEY).stream()
        .map(JsonObject.class::cast)
        .map(EventLogEntry::new)
        .collect(Collectors.toList());
  }

  public JsonObject toJson() {
    final JsonArray jsonArray = new JsonArray();
    operations.forEach(entry -> jsonArray.add(entry.toJson()));
    return new JsonObject()
        .put(OPERATIONS_KEY, jsonArray);
  }

  void append(EventLogEntry logEntry) {
    operations.add(logEntry);
  }

  public JsonObject getLog() {
    return toJson();
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
