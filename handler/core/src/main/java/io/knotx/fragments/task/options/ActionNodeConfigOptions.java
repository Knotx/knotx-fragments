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
package io.knotx.fragments.task.options;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Action node configuration model. It is model for {@link NodeOptions#getConfig()} JSON object.
 *
 * <pre>
 * node {
 *   factory = action
 *   config {
 *     HERE
 *   }
 * }
 * </pre>
 */
@DataObject(generateConverter = true)
public class ActionNodeConfigOptions {

  private String action;


  ActionNodeConfigOptions(String action) {
    setAction(action);
  }

  public ActionNodeConfigOptions(JsonObject json) {
    ActionNodeConfigOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ActionNodeConfigOptionsConverter.toJson(this, json);
    return json;
  }


  /**
   * {@link io.knotx.fragments.handler.api.Action} name
   *
   * @return Action name
   */
  public String getAction() {
    return action;
  }

  /**
   * Sets {@link io.knotx.fragments.handler.api.Action} name. The specified Action is executed
   * during processing of given graph node.
   *
   * @param action action name
   * @return reference to this, so the API can be used fluently
   */
  public ActionNodeConfigOptions setAction(String action) {
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
    ActionNodeConfigOptions that = (ActionNodeConfigOptions) o;
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
