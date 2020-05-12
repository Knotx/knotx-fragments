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
package io.knotx.fragments.action.core;

import io.knotx.fragments.action.api.Action;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.Map;
import java.util.Optional;

/**
 * Options used during {@link ActionProvider#ActionProvider(Map, Vertx)} initialization. Those
 * values are passed then to {@link io.knotx.fragments.action.api.ActionFactory#create(String,
 * JsonObject, io.vertx.core.Vertx, Action)}.
 */
@DataObject(generateConverter = true)
public class ActionFactoryOptions {

  private String factory;
  private JsonObject config = new JsonObject();
  private String doAction;

  public ActionFactoryOptions() {
    // default
  }

  public ActionFactoryOptions(String factory, JsonObject config, String doAction) {
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
   * Sets the {@code Action} (by its alias) that would be triggered by the returned action instance.
   * This is mainly used by <a href="https://github.com/Knotx/knotx-fragments/tree/master/action#behaviours">behaviours</a>.
   *
   * @param doAction name of the base {@code Action}.
   * @return reference to this, so the API can be used fluently
   */
  public ActionFactoryOptions setDoAction(String doAction) {
    this.doAction = doAction;
    return this;
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
