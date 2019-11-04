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

import java.util.Arrays;

public enum ActionLogLevel {
  INFO("info"), ERROR("error");

  public static final String CONFIG_KEY_NAME = "logLevel";
  private final String level;

  ActionLogLevel(String level) {
    this.level = level;
  }

  public String getLevel() {
    return level;
  }

  public static ActionLogLevel fromConfig(String level) {
    return Arrays.asList(ActionLogLevel.values())
        .stream()
        .filter(al -> level.equals(al.getLevel()))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            format("Incorrect action log level: %s", level)));
  }
}
