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

import static io.knotx.fragments.handler.api.actionlog.ActionLogMode.INFO;

import java.time.Instant;
import java.util.function.Function;

import io.knotx.fragments.handler.api.ActionConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ActionLogger {

  private final ActionLogMode actionLogMode;
  private final JsonObject logs;
  private final JsonObject actionLog;

  private ActionLogger(String alias, ActionLogMode actionLogMode) {
    this.actionLogMode = actionLogMode;
    this.logs = new JsonObject();
    this.actionLog = new JsonObject().put("alias", alias)
        .put("logs", this.logs)
        .put("doAction", new JsonArray());
  }

  public static ActionLogger create(String alias, ActionLogMode actionLogMode) {
    return new ActionLogger(alias, actionLogMode);
  }

  public static ActionLogger create(ActionConfig actionConfig) {
    return new ActionLogger(actionConfig.getAlias(), actionConfig.getActionLogMode());
  }

  public void info(String key, JsonObject data) {
    if (actionLogMode == INFO) {
      this.logs.put(key, data);
    }
  }

  public void info(String key, Object data) {
    if (actionLogMode == INFO) {
      this.logs.put(key, data);
    }
  }

  public <T> void info(String key, T data, Function<T, JsonObject> toJsonFunc) {
    if (actionLogMode == INFO) {
      this.logs.put(key, toJsonFunc.apply(data));
    }
  }

  public void info(String key, String data) {
    if (actionLogMode == INFO) {
      this.logs.put(key, data);
    }
  }

  public void doActionLog(JsonObject actionLog) {
    this.actionLog.getJsonArray("doAction").add(actionLog);
  }

  public void error(String key, JsonObject data) {
    this.logs.put(key, data);
  }

  public void error(String key, String data) {
    this.logs.put(key, data);
  }

  public void error(String data) {
    this.logs.put(String.valueOf(Instant.now().toEpochMilli()), data);
  }

  public <T> void error(String key, T data, Function<T, JsonObject> toJsonFunc) {
    this.logs.put(key, toJsonFunc.apply(data));
  }

  public JsonObject toLog() {
    return new JsonObject(this.actionLog.getMap());
  }

  public static String getStringLogEntry(String key, JsonObject actionLog){
    return getLogs(actionLog).getString(key);
  }

  public static JsonObject getLogEntry(String key, JsonObject actionLog){
    return getLogs(actionLog).getJsonObject(key);
  }

  public static JsonObject getLogs(JsonObject actionLog){
    return actionLog.getJsonObject("logs");
  }
}