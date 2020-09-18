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

import io.vertx.codegen.annotations.DataObject;
import java.util.Objects;
import java.util.Optional;

import io.vertx.core.json.JsonObject;

@DataObject
/**
 * This is an enveloped action invocation log.
 */
public class ActionInvocationLog {

  public static final String DURATION = "duration";
  public static final String SUCCESS = "success";
  public static final String LOG = "log";

  /**
   * Action processing time in milliseconds.
   */
  private final Long duration;

  /**
   * <code>true</code> when action ends with success
   */
  private final boolean success;

  /**
   * Enveloped action log.
   */
  private final ActionLog log;

  private ActionInvocationLog(long duration, boolean success, ActionLog log) {
    this.duration = duration;
    this.success = success;
    this.log = log;
  }

  public ActionInvocationLog(JsonObject json) {
    this.duration = json.getLong(DURATION);
    this.success = json.getBoolean(SUCCESS);
    this.log = toLog(json);
  }

  static ActionInvocationLog success(long duration, ActionLog actionLog) {
    return new ActionInvocationLog(duration, true, actionLog);
  }

  static ActionInvocationLog error(long duration, ActionLog actionLog) {
    return new ActionInvocationLog(duration, false, actionLog);
  }

  public Long getDuration() {
    return duration;
  }

  public boolean isSuccess() {
    return success;
  }

  public ActionLog getLog() {
    return log;
  }

  public JsonObject toJson() {
    return new JsonObject().put(DURATION, duration)
        .put(SUCCESS, success)
        .put(LOG, log != null ? log.toJson() : null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionInvocationLog that = (ActionInvocationLog) o;
    return success == that.success &&
        Objects.equals(duration, that.duration) &&
        Objects.equals(log, that.log);
  }

  @Override
  public int hashCode() {
    return Objects.hash(duration, success, log);
  }

  @Override
  public String toString() {
    return "ActionInvocationLog{" +
        "duration=" + duration +
        ", success=" + success +
        ", log=" + log +
        '}';
  }

  private ActionLog toLog(JsonObject json) {
    return Optional.ofNullable(json)
        .map(j -> j.getJsonObject(LOG))
        .map(ActionLog::new)
        .orElse(null);
  }
}
