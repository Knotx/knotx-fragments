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
import java.util.stream.Collectors;

class FragmentEventLog {

  private final List<LogEntry> operations;

  FragmentEventLog() {
    operations = new ArrayList<>();
  }

  FragmentEventLog(JsonObject json) {
    operations = json.getJsonArray("operations").stream()
        .map(JsonObject.class::cast)
        .map(LogEntry::new)
        .collect(Collectors.toList());
  }

  public JsonObject toJson() {
    final JsonArray jsonArray = new JsonArray();
    operations.forEach(entry -> jsonArray.add(entry.toJson()));
    return new JsonObject()
        .put("operations", jsonArray);
  }

  List<LogEntry> getOperations() {
    return operations;
  }

  void append(String consumerId, String transition) {
    operations.add(new LogEntry(consumerId, transition));
  }

  private class LogEntry {

    private String consumerId;
    private String transition;
    private long timestamp;

    private LogEntry(String consumerId, String transition) {
      this.consumerId = consumerId;
      this.transition = transition;
      this.timestamp = System.currentTimeMillis();
    }

    LogEntry(JsonObject json) {
      this.consumerId = json.getString("consumerId");
      this.transition = json.getString("transition");
      this.timestamp = json.getLong("timestamp");
    }

    JsonObject toJson() {
      return new JsonObject()
          .put("consumerId", consumerId)
          .put("transition", transition)
          .put("timestamp", timestamp);
    }

    public String getConsumerId() {
      return consumerId;
    }

    public String getTransition() {
      return transition;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
