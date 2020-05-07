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
package io.knotx.fragments.task.factory.config;

import static io.knotx.fragments.action.api.log.ActionLogLevel.ERROR;

import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class LogLevelConfig {

  private String logLevel;

  public LogLevelConfig() {
    logLevel = ActionLogLevel.ERROR.getLevel();
  }

  public LogLevelConfig(JsonObject json) {
    LogLevelConfigConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    LogLevelConfigConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public static JsonObject override(JsonObject json, String defaultLogLevel) {
    if (!StringUtils.isBlank(defaultLogLevel)) {
      LogLevelConfig logLevelConfig = new LogLevelConfig(json);
      if (StringUtils.isBlank(logLevelConfig.getLogLevel())) {
        json.mergeIn(logLevelConfig.setLogLevel(defaultLogLevel).toJson());
      }
    }
    return json;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public LogLevelConfig setLogLevel(String logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LogLevelConfig that = (LogLevelConfig) o;
    return Objects.equals(logLevel, that.logLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(logLevel);
  }

  @Override
  public String toString() {
    return "LogLevelConfig{" +
        "logLevel='" + logLevel + '\'' +
        '}';
  }
}
