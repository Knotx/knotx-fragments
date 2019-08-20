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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.engine;

import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.json.JsonObject;

public class EventLogEntry {

  private static final String TASK_KEY = "task";
  private static final String ACTION_KEY = "action";
  private static final String STATUS_KEY = "status";
  private static final String TRANSITION_KEY = "transition";
  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String ACTION_LOG_KEY = "actionLog";

  private final String task;
  private final String action;
  private final ActionStatus status;
  private final String transition;
  private final long timestamp;
  private final JsonObject actionLog;

  public static EventLogEntry success(String task, String action, FragmentResult fragmentResult) {
    return new EventLogEntry(task, action, ActionStatus.SUCCESS, fragmentResult.getTransition(), fragmentResult.getActionLog());
  }

  public static EventLogEntry unsupported(String task, String action, String transition) {
    return new EventLogEntry(task, action, ActionStatus.UNSUPPORTED_TRANSITION, transition,null);
  }

  public static EventLogEntry error(String task, String action, String transition, JsonObject actionLog) {
    return new EventLogEntry(task, action, ActionStatus.ERROR, transition, actionLog);
  }

  public static EventLogEntry error(String task, String action, String transition) {
    return new EventLogEntry(task, action, ActionStatus.ERROR, transition,null);
  }

  public static EventLogEntry timeout(String task, String action) {
    return new EventLogEntry(task, action, ActionStatus.TIMEOUT, null, null);
  }

  private EventLogEntry(String task, String action, ActionStatus status, String transition, JsonObject actionLog) {
    this.task = task;
    this.action = action;
    this.status = status;
    this.transition = transition;
    this.timestamp = System.currentTimeMillis();
    this.actionLog = actionLog;
  }

  EventLogEntry(JsonObject json) {
    this.task = json.getString(TASK_KEY);
    this.action = json.getString(ACTION_KEY);
    this.status = ActionStatus.valueOf(json.getString(STATUS_KEY));
    this.transition = json.getString(TRANSITION_KEY);
    this.timestamp = json.getLong(TIMESTAMP_KEY);
    this.actionLog = json.getJsonObject(ACTION_LOG_KEY);
  }

  JsonObject toJson() {
    return new JsonObject()
        .put(TASK_KEY, task)
        .put(ACTION_KEY, action)
        .put(STATUS_KEY, status.name())
        .put(TRANSITION_KEY, transition)
        .put(TIMESTAMP_KEY, timestamp)
        .put(ACTION_LOG_KEY, actionLog);
  }

  @Override
  public String toString() {
    return "EventLogEntry{" +
        "task='" + task + '\'' +
        ", action='" + action + '\'' +
        ", status=" + status +
        ", transition='" + transition + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }

  enum ActionStatus {
    SUCCESS,
    UNSUPPORTED_TRANSITION,
    ERROR,
    TIMEOUT //?
  }

}
