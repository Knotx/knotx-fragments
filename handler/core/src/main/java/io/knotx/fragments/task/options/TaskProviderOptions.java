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

@DataObject(generateConverter = true)
public class TaskProviderOptions {

  private String name;

  private JsonObject config;

  public TaskProviderOptions(String factory, JsonObject config) {
    this.name = factory;
    this.config = config;
  }

  public TaskProviderOptions(JsonObject json) {
    TaskProviderOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    TaskProviderOptionsConverter.toJson(this, result);
    return result;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JsonObject getConfig() {
    return config;
  }

  public void setConfig(JsonObject config) {
    this.config = config;
  }

  @Override
  public String toString() {
    return "TaskBuilderOptions{" +
        "name='" + name + '\'' +
        ", config=" + config +
        '}';
  }
}
