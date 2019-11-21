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
package io.knotx.fragments.task.factory.node.action;

import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.task.exception.NodeConfigException;
import io.knotx.fragments.task.factory.config.LogLevelConfig;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@DataObject(generateConverter = true)
public class ActionNodeFactoryConfig {

  private Map<String, ActionOptions> actions;

  public ActionNodeFactoryConfig(Map<String, ActionOptions> actions) {
    this.actions = actions;
  }

  public ActionNodeFactoryConfig(JsonObject json) {
    ActionNodeFactoryConfigConverter.fromJson(json, this);
    initActionLogLevel(json);
  }

  private void initActionLogLevel(JsonObject json) {
    LogLevelConfig defaultLogLevel = new LogLevelConfig(json);
    Optional.ofNullable(actions).map(actions -> {
      actions.values().forEach(actionOptions -> {
        JsonObject actionConfig = actionOptions.getConfig();
        LogLevelConfig.override(actionConfig, defaultLogLevel.getLogLevel());
      });
      return actions;
    }).orElseThrow(() -> new NodeConfigException(json));
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    ActionNodeFactoryConfigConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public Map<String, ActionOptions> getActions() {
    return actions;
  }

  public ActionNodeFactoryConfig setActions(
      Map<String, ActionOptions> actions) {
    this.actions = actions;
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
    ActionNodeFactoryConfig that = (ActionNodeFactoryConfig) o;
    return Objects.equals(actions, that.actions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actions);
  }

  @Override
  public String toString() {
    return "ActionsConfig{" +
        "actions=" + actions +
        '}';
  }
}
