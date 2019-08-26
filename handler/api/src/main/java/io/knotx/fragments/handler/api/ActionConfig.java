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
package io.knotx.fragments.handler.api;

import static io.knotx.fragments.handler.api.actionlog.ActionLogMode.ERROR;

import io.knotx.fragments.handler.api.actionlog.ActionLogMode;
import io.vertx.core.json.JsonObject;

public class ActionConfig {
  private final String alias;
  private final Action doAction;
  private final ActionLogMode actionLogMode;
  private final JsonObject options;

  public ActionConfig(String alias, Action doAction, JsonObject options, ActionLogMode actionLogMode) {
    this.alias = alias;
    this.options = options;
    this.doAction = doAction;
    this.actionLogMode = actionLogMode;
  }
  public ActionConfig(String alias, JsonObject options) {
    this(alias, null, options, ERROR);
  }

  public JsonObject getOptions() {
    return options;
  }

  public Action getDoAction() {
    return doAction;
  }

  public ActionLogMode getActionLogMode() {
    return actionLogMode;
  }

  public String getAlias() {
    return alias;
  }

  public boolean hasAction(){
    return doAction != null;
  }
}
