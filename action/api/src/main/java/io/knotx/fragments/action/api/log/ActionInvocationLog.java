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

import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.api.invoker.ActionInvocation.Status;
import io.knotx.fragments.api.FragmentOperationFailure;
import io.vertx.codegen.annotations.DataObject;
import java.util.Arrays;
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
  public static final String TRANSITION = "transition";
  public static final String STATUS = "status";
  public static final String ERROR = "error";
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
   * Transition associated with resulting FragmentResult
   */
  private final String transition;

  /**
   * Status indicating how invocation ended
   */
  private final ActionInvocation.Status status;

  /**
   * Error if thrown when invocation
   */
  private final FragmentOperationFailure error;

  /**
   * Enveloped action log.
   */
  private final ActionLog log;

  public ActionInvocationLog(Long duration, boolean success, String transition,
      Status status, FragmentOperationFailure error,
      ActionLog log) {
    this.duration = duration;
    this.success = success;
    this.transition = transition;
    this.status = status;
    this.error = error;
    this.log = log;
  }

  public ActionInvocationLog(JsonObject json) {
    this.duration = json.getLong(DURATION);
    this.success = json.getBoolean(SUCCESS);
    this.transition = json.getString(TRANSITION);
    this.status = toStatus(json.getString(STATUS));
    this.error = toError(json.getJsonObject(ERROR));
    this.log = toLog(json.getJsonObject(LOG));
  }

  private Status toStatus(String status) {
    return Arrays.stream(Status.values())
        .filter(candidate -> candidate.name().equals(status))
        .findAny()
        .orElse(null);
  }

  private FragmentOperationFailure toError(JsonObject json) {
    return Optional.ofNullable(json)
        .map(FragmentOperationFailure::new)
        .orElse(null);
  }

  static ActionInvocationLog success(ActionInvocation invocation) {
    return new ActionInvocationLog(invocation.getDuration(),
        true,
        invocation.getFragmentResult().getTransition(),
        invocation.getStatus(),
        invocation.getFragmentResult().getError(),
        toLog(invocation.getFragmentResult().getLog()));
  }

  static ActionInvocationLog error(ActionInvocation invocation) {
    return new ActionInvocationLog(invocation.getDuration(),
        false,
        invocation.getFragmentResult().getTransition(),
        invocation.getStatus(),
        invocation.getFragmentResult().getError(),
        toLog(invocation.getFragmentResult().getLog()));
  }

  public boolean isSuccess() {
    return success;
  }

  public ActionLog getLog() {
    return log;
  }

  public JsonObject toJson() {
    JsonObject output = new JsonObject().put(DURATION, duration)
        .put(SUCCESS, success)
        .put(TRANSITION, transition);
    if(status != null) {
      output.put(STATUS, status.name());
    }
    if(error != null) {
      output.put(ERROR, error.toJson());
    }
    if(log != null) {
      output.put(LOG, log.toJson());
    }
    return output;
  }

  private static ActionLog toLog(JsonObject json) {
    return Optional.ofNullable(json)
        .map(ActionLog::new)
        .orElse(null);
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
        Objects.equals(transition, that.transition) &&
        status == that.status &&
        Objects.equals(error, that.error) &&
        Objects.equals(log, that.log);
  }

  @Override
  public int hashCode() {
    return Objects.hash(duration, success, transition, status, error, log);
  }

  @Override
  public String toString() {
    return "ActionInvocationLog{" +
        "duration=" + duration +
        ", success=" + success +
        ", transition='" + transition + '\'' +
        ", status=" + status +
        ", error=" + error +
        ", log=" + log +
        '}';
  }
}
