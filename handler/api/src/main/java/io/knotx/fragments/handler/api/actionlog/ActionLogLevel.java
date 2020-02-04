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

import static java.lang.String.format;

import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public enum ActionLogLevel {
  INFO("info"), ERROR("error");

  private static final String CONFIG_KEY_NAME = "logLevel";
  private final String level;

  ActionLogLevel(String level) {
    this.level = level;
  }

  public String getLevel() {
    return level;
  }

  public static ActionLogLevel fromConfig(JsonObject config) {
    return fromConfig(config.getString(CONFIG_KEY_NAME, ERROR.level));
  }

  public static ActionLogLevel fromConfig(JsonObject config, ActionLogLevel defaultLevel) {
    String level = config.getString(CONFIG_KEY_NAME);
    return fromConfig(level, defaultLevel);
  }

  public static ActionLogLevel fromConfig(String level, ActionLogLevel defaultLevel) {
    if (StringUtils.isBlank(level)) {
      return defaultLevel;
    } else {
      return fromConfig(level);
    }
  }

  public static ActionLogLevel fromConfig(String level) {
    return Arrays.stream(ActionLogLevel.values())
        .filter(al -> Objects.equals(level, al.getLevel()))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            format("Incorrect action log level: %s", level)));
  }
}
