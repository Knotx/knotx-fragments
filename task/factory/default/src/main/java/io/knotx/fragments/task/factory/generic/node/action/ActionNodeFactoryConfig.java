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
package io.knotx.fragments.task.factory.generic.node.action;


import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.action.core.ActionProvider;
import io.knotx.fragments.task.factory.generic.LogLevelConfig;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action Node factory config model.
 */
@DataObject(generateConverter = true)
public class ActionNodeFactoryConfig {

  private Map<String, ActionFactoryOptions> actions;

  public ActionNodeFactoryConfig(Map<String, ActionFactoryOptions> actions) {
    this(actions, ActionLogLevel.fromConfig(new LogLevelConfig().toJson()));
  }

  public ActionNodeFactoryConfig(Map<String, ActionFactoryOptions> actions, ActionLogLevel globalLogLevel) {
    this.actions = actions;
    initActionLogLevel(globalLogLevel.getLevel());
  }

  public ActionNodeFactoryConfig(JsonObject json) {
    actions = new HashMap<>();
    ActionNodeFactoryConfigConverter.fromJson(json, this);
    LogLevelConfig globalLogLevel = new LogLevelConfig(json);
    initActionLogLevel(globalLogLevel.getLogLevel());
  }

  private void initActionLogLevel(String logLevel) {
    actions.values().forEach(actionOptions -> {
      JsonObject actionConfig = actionOptions.getConfig();
      LogLevelConfig.override(actionConfig, logLevel);
    });
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    ActionNodeFactoryConfigConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public Map<String, ActionFactoryOptions> getActions() {
    return actions;
  }

  /**
   * The dictionary maps action name to action factory options.
   *
   * @param actions map of actions
   * @return reference to this, so the API can be used fluently
   * @see ActionProvider#get(String)
   */
  public ActionNodeFactoryConfig setActions(Map<String, ActionFactoryOptions> actions) {
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
