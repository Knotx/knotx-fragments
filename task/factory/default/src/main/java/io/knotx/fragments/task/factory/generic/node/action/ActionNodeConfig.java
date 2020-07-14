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

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.task.factory.generic.node.NodeOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Action node configuration model. It is model for {@link NodeOptions#getConfig()} JSON object.
 *
 * <pre>
 * node {
 *   factory = action
 *   config { //represented by ActionNodeConfig
 *     ...
 *   }
 * }
 * </pre>
 */
@DataObject(generateConverter = true)
public class ActionNodeConfig {

  private String action;

  public ActionNodeConfig(String action) {
    setAction(action);
  }

  public ActionNodeConfig(JsonObject json) {
    ActionNodeConfigConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ActionNodeConfigConverter.toJson(this, json);
    return json;
  }


  /**
   * {@link Action} name
   *
   * @return Action name
   */
  public String getAction() {
    return action;
  }

  /**
   * Sets {@link Action} name. The specified Action is executed
   * during processing of given graph node.
   *
   * @param action action name
   * @return reference to this, so the API can be used fluently
   */
  public ActionNodeConfig setAction(String action) {
    this.action = action;
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
    ActionNodeConfig that = (ActionNodeConfig) o;
    return Objects.equals(action, that.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action);
  }

  @Override
  public String toString() {
    return "ActionNodeOptions{" +
        "action='" + action + '\'' +
        '}';
  }
}
