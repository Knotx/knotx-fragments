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
package io.knotx.fragments.handler.action;

import java.util.Objects;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ActionOptions {

  private String factory;
  private JsonObject config;
  private String doAction;

  ActionOptions(String factory, JsonObject config) {
    this(factory, config, null);
  }

  ActionOptions(String factory, JsonObject config, String doAction) {
    this.factory = factory;
    this.config = config;
    this.doAction = doAction;
  }


  public ActionOptions(JsonObject json) {
    ActionOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ActionOptionsConverter.toJson(this, json);
    return json;
  }

  public String getFactory() {
    return factory;
  }

  /**
   * Sets {@code Action} factory name.
   *
   * @param factory action factory name.
   * @return reference to this, so the API can be used fluently
   */
  public ActionOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return config;
  }

  /**
   * Sets {@code Action} configuration that is passed to Action.
   *
   * @param config action factory configuration.
   * @return reference to this, so the API can be used fluently
   */
  public ActionOptions setConfig(JsonObject config) {
    this.config = config;
    return this;
  }

  public String getDoAction() {
    return doAction;
  }

  /**
   * Sets the name of the base {@code Action} that will be triggered while creating current {@code
   * Action}. In not set ({@code null}), given action will have no base actions.
   *
   * @param doAction name of the base {@code Action}.
   * @return reference to this, so the API can be used fluently
   */
  public ActionOptions setDoAction(String doAction) {
    this.doAction = doAction;
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
    ActionOptions that = (ActionOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config) &&
        Objects.equals(doAction, that.doAction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config, doAction);
  }

  @Override
  public String toString() {
    return "ActionOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        ", doAction='" + doAction + '\'' +
        '}';
  }
}
