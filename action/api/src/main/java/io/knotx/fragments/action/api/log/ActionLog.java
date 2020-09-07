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
package io.knotx.fragments.action.api.log;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.stream.StreamSupport;

@DataObject
public class ActionLog {

  public static final String ALIAS = "alias";
  public static final String LOGS = "logs";
  public static final String INVOCATIONS = "invocations";

  /**
   * Configurable action name.
   */
  private final String alias;
  /**
   * Action log data, e.g HTTP action adds HTTP request and response details.
   */
  private final JsonObject logs;
  /**
   * Behaviours envelop other actions. They can invoke enveloped actions many times (in case of
   * failure). This is an array that holds details about these invocations.
   */
  private final List<ActionInvocationLog> invocations;

  public ActionLog(String alias, JsonObject logs, List<ActionInvocationLog> invocations) {
    this.alias = alias;
    this.logs = logs;
    this.invocations = invocations;
  }

  public ActionLog(JsonObject actionLog) {
    this.alias = actionLog.getString(ALIAS, "");
    this.logs = actionLog.getJsonObject(LOGS, new JsonObject());
    this.invocations = toInvocationLogList(actionLog);
  }

  private List<ActionInvocationLog> toInvocationLogList(JsonObject actionLog) {
    JsonArray invocations = actionLog.getJsonArray(INVOCATIONS, new JsonArray());
    return StreamSupport.stream(invocations.spliterator(), false)
        .map(JsonObject::mapFrom)
        .map(ActionInvocationLog::new)
        .collect(toList());
  }

  public String getAlias() {
    return alias;
  }

  public JsonObject getLogs() {
    return logs.copy();
  }

  public List<ActionInvocationLog> getInvocationLogs() {
    return unmodifiableList(invocations);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(ALIAS, alias)
        .put(LOGS, getLogs())
        .put(INVOCATIONS, toInvocationsArray());
  }

  @Override
  public String toString() {
    return "ActionLog{" +
        "alias='" + alias + '\'' +
        ", logs=" + logs +
        ", invocations=" + invocations +
        '}';
  }

  private JsonArray toInvocationsArray() {
    return getInvocationLogs().stream()
        .map(ActionInvocationLog::toJson)
        .collect(JsonArray::new,
            JsonArray::add,
            JsonArray::addAll);
  }
}
