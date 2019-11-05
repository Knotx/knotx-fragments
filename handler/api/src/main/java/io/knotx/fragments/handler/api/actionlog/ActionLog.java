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
package io.knotx.fragments.handler.api.actionlog;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
public class ActionLog {

  private final String alias;
  private final JsonObject logs;
  private final List<ActionLog> doActionLogs;

  ActionLog(String alias, JsonObject logs, List<ActionLog> doActionLogs) {
    this.alias = alias;
    this.logs = logs;
    this.doActionLogs = doActionLogs;
  }

  public ActionLog(JsonObject actionLog) {
    this.alias = actionLog.getString("alias");
    this.logs = actionLog.getJsonObject("logs");
    this.doActionLogs = toDoActionLogs(actionLog);
  }

  private List<ActionLog> toDoActionLogs(JsonObject actionLog) {
    Iterable<Object> iterable = () -> actionLog.getJsonArray("doAction").iterator();
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(JsonObject::mapFrom)
        .map(ActionLog::new)
        .collect(toList());
  }

  public String getAlias() {
    return alias;
  }

  public JsonObject getLogs() {
    return logs.copy();
  }

  public List<ActionLog> getDoActionLogs() {
    return unmodifiableList(doActionLogs);
  }

  public JsonObject toJson() {
    return new JsonObject().put("alias", alias).put("logs", getLogs())
        .put("doAction", toDoActionArray());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionLog actionLog = (ActionLog) o;
    return alias.equals(actionLog.alias) &&
        logs.equals(actionLog.logs) &&
        Objects.equals(doActionLogs, actionLog.doActionLogs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alias, logs, doActionLogs);
  }

  @Override
  public String toString() {
    return "ActionLog{" +
        "alias='" + alias + '\'' +
        ", log=" + logs +
        ", doActionLogs=" + doActionLogs +
        '}';
  }

  private JsonArray toDoActionArray() {
    return getDoActionLogs().stream()
        .map(ActionLog::toJson)
        .collect(JsonArray::new,
            JsonArray::add,
            JsonArray::addAll);
  }
}