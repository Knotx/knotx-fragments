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

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.CONFIG_KEY_NAME;
import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.INFO;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

import io.vertx.core.json.JsonObject;

public class ActionLogger {

  private final ActionLogLevel actionLogLevel;
  private final ActionLogBuilder builder;

  private ActionLogger(String alias, ActionLogLevel actionLogLevel) {
    this.actionLogLevel = actionLogLevel;
    this.builder = new ActionLogBuilder(alias);
  }

  public static ActionLogger create(String alias, JsonObject config) {
    return ActionLogger.create(alias, config.getString(CONFIG_KEY_NAME));
  }

  public static ActionLogger create(String alias, ActionLogLevel actionLogLevel) {
    return new ActionLogger(alias, actionLogLevel);
  }

  public static ActionLogger create(String alias, String actionLogLevel) {
    ActionLogLevel logLevel = ActionLogLevel.fromConfig(actionLogLevel);
    return new ActionLogger(alias, logLevel);
  }

  public void info(String key, Object data) {
    if (actionLogLevel == INFO) {
      if(data instanceof String){
        builder.addLog(key, String.valueOf(data));
        return;
      }
      this.builder.addLog(key, JsonObject.mapFrom(data));
    }
  }

  public void info(String key, JsonObject data) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, data);
    }
  }

  public <T> void info(String key, T data, Function<T, JsonObject> toJsonFunc) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, toJsonFunc.apply(data));
    }
  }

  public void info(String key, String data) {
    if (actionLogLevel == INFO) {
      this.builder.addLog(key, data);
    }
  }

  public void doActionLog(ActionLog actionLog) {
    if(Objects.isNull(actionLog)){
      return;
    }
    this.builder.addActionLog(actionLog);
  }

  public void error(String key, JsonObject data) {
    this.builder.addLog(key, data);
  }

  public void error(String key, String data) {
    this.builder.addLog(key, data);
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
}