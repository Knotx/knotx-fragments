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
package io.knotx.fragments.task.factory;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import java.util.Optional;

@DataObject(generateConverter = true)
public class ActionFactoryOptions {

  private String factory;
  private JsonObject config;
  private String doAction;

  private ActionFactoryOptions() {
  }

  ActionFactoryOptions(String factory) {
    this(factory, null, null);
  }

  ActionFactoryOptions(String factory, JsonObject config) {
    this(factory, config, null);
  }

  ActionFactoryOptions(String factory, JsonObject config, String doAction) {
    ActionFactoryOptions actionFactoryOptions = new ActionFactoryOptions().setFactory(factory)
        .setConfig(config)
        .setDoAction(doAction);

    JsonObject json = new JsonObject();
    ActionFactoryOptionsConverter.toJson(actionFactoryOptions, json);
    ActionFactoryOptionsConverter.fromJson(json, this);
  }

  public ActionFactoryOptions(JsonObject json) {
    ActionFactoryOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ActionFactoryOptionsConverter.toJson(this, json);
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
  public ActionFactoryOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return Optional.ofNullable(config).orElse(new JsonObject());
  }

  /**
   * Sets {@code Action} configuration that is passed to Action.
   *
   * @param config action factory configuration.
   * @return reference to this, so the API can be used fluently
   */
  public ActionFactoryOptions setConfig(JsonObject config) {
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
  public ActionFactoryOptions setDoAction(String doAction) {
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
    ActionFactoryOptions that = (ActionFactoryOptions) o;
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
