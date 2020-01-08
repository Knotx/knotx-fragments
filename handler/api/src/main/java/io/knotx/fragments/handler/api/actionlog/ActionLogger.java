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

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.INFO;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.function.Function;

public class ActionLogger {

  private final ActionLogLevel actionLogLevel;
  private final ActionLogBuilder builder;

  private ActionLogger(String alias, ActionLogLevel actionLogLevel) {
    this.actionLogLevel = actionLogLevel;
    this.builder = new ActionLogBuilder(alias);
  }

  public static ActionLogger create(String alias, ActionLogLevel actionLogLevel) {
    return new ActionLogger(alias, actionLogLevel);
  }

  public static ActionLogger create(String alias, String logLevel) {
    return new ActionLogger(alias, ActionLogLevel.fromConfig(logLevel));
  }

  public void info(String key, String value) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, value);
    }
  }

  public void info(String key, JsonObject value) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, value);
    }
  }

  public void info(String key, JsonArray value) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, value);
    }
  }

  public <T> void info(String key, T value, Function<T, JsonObject> toJsonFunc) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, toJsonFunc.apply(value));
    }
  }

  public void error(String key, String value) {
    this.builder.addLog(key, value);
  }

  public void error(String key, JsonArray value) {
    this.builder.addLog(key, value);
  }

  public void error(String key, JsonObject value) {
    this.builder.addLog(key, value);
  }

  public void error(Throwable throwable) {
    this.builder.addLog("_error",
        new JsonObject().put("className", throwable.getClass().getCanonicalName())
            .put("message", throwable.getMessage()));
  }

  public void error(String data) {
    this.builder.addLog(String.valueOf(Instant.now().toEpochMilli()), data);
  }

  public <T> void error(String key, T data, Function<T, JsonObject> toJsonFunc) {
    this.builder.addLog(key, toJsonFunc.apply(data));
  }

  public ActionLog toLog() {
    return builder.build();
  }

  public void doActionLog(long duration, JsonObject actionLog) {
    if (actionLogLevel == INFO) {
      this.builder.appendInvocationLogEntry(duration, toActionLogOrNull(actionLog));
    }
  }

  public void failureDoActionLog(long duration, JsonObject actionLog) {
    this.builder.appendFailureInvocationLogEntry(duration, toActionLogOrNull(actionLog));
  }

  private ActionLog toActionLogOrNull(JsonObject jsonObject) {
    return jsonObject != null ? new ActionLog(jsonObject) : null;
  }
}