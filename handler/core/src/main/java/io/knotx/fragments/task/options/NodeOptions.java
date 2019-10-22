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

@DataObject(generateConverter = true)
public class NodeOptions {

  private String factory;
  private JsonObject config;

  public NodeOptions() {
    init();
  }

  public NodeOptions(JsonObject json) {
    init();
    NodeOptionsConverter.fromJson(json, this);
  }

  private void init() {
    config = new JsonObject();
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    NodeOptionsConverter.toJson(this, result);
    return result;
  }

  public String getFactory() {
    return factory;
  }

  public NodeOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return config;
  }

  public NodeOptions setConfig(JsonObject config) {
    this.config = config;
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
    NodeOptions that = (NodeOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config);
  }

  @Override
  public String toString() {
    return "NodeOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        '}';
  }
}
