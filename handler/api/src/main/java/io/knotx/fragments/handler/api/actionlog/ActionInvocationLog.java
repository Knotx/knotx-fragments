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

import java.util.Optional;

import io.vertx.core.json.JsonObject;

public class ActionInvocationLog {

  private final Long duration;
  private final boolean success;
  private final ActionLog doActionLog;

  private ActionInvocationLog(long duration, boolean success, ActionLog doActionLog) {
    this.duration = duration;
    this.success = success;
    this.doActionLog = doActionLog;
  }

  ActionInvocationLog(JsonObject json) {
    this.duration = json.getLong("duration");
    this.success = json.getBoolean("success");
    this.doActionLog = toDoActionLog(json);
  }

  private ActionLog toDoActionLog(JsonObject json) {
    return Optional.ofNullable(json)
        .map(j -> j.getJsonObject("doActionLog"))
        .map(ActionLog::new)
        .orElse(null);
  }

  public Long getDuration() {
    return duration;
  }

  public boolean isSuccess() {
    return success;
  }

  public ActionLog getDoActionLog() {
    return doActionLog;
  }

  public JsonObject toJson() {
    return new JsonObject().put("duration", duration)
        .put("success", success)
        .put("doActionLog", toDoActionLogJson());
  }

  private JsonObject toDoActionLogJson() {
    return Optional.ofNullable(doActionLog).map(ActionLog::toJson).orElse(null);
  }

  static ActionInvocationLog success(long duration, ActionLog actionLog) {
    return new ActionInvocationLog(duration, true, actionLog);
  }

  static ActionInvocationLog error(long duration,  ActionLog actionLog) {
    return new ActionInvocationLog(duration, false, actionLog);
  }
}