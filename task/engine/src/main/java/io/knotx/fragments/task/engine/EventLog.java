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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventLog {

  private final List<EventLogEntry> operations;

  public EventLog() {
    operations = new ArrayList<>();
  }

  public EventLog(List<EventLogEntry> operations) {
    this.operations = operations;
  }

  void append(EventLogEntry logEntry) {
    operations.add(logEntry);
  }

  void appendAll(EventLog log) {
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
