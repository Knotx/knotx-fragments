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
package io.knotx.fragments.task.factory.node;

import io.knotx.fragments.task.factory.node.action.ActionProvider;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Node Factory options model.
 */
@DataObject(generateConverter = true)
public class NodeFactoryOptions {

  private String factory;
  private JsonObject config;

  public NodeFactoryOptions() {
    config = new JsonObject();
  }

  public NodeFactoryOptions(JsonObject json) {
    this();
    NodeFactoryOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    NodeFactoryOptionsConverter.toJson(this, result);
    return result;
  }

  public String getFactory() {
    return factory;
  }

  /**
   * The node factory name that identifies {@code NodeFactory} implementation.
   *
   * @param factory - node factory name
   * @return reference to this, so the API can be used fluently
   */
  public NodeFactoryOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return config;
  }

  /**
   * The JSON object that contains node factory configuration entries.
   *
   * @param config - node factory config
   * @return reference to this, so the API can be used fluently
   */
  public NodeFactoryOptions setConfig(JsonObject config) {
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
    NodeFactoryOptions that = (NodeFactoryOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config);
  }

  @Override
  public String toString() {
    return "NodeFactoryOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        '}';
  }
}
