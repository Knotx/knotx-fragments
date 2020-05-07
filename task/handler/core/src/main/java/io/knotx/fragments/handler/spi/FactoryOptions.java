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
package io.knotx.fragments.handler.spi;

import io.knotx.fragments.handler.exception.ConfigurationException;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class FactoryOptions {

  private String factory;
  private JsonObject config;

  public FactoryOptions(JsonObject json) {
    FactoryOptionsConverter.fromJson(json, this);
    if (StringUtils.isBlank(factory)) {
      throw new ConfigurationException("Factory name not defined in configuration [" + json + "]!");
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    FactoryOptionsConverter.toJson(this, json);
    return json;
  }

  public String getFactory() {
    return factory;
  }

  /**
   * The factory name.
   *
   * @param factory - factory name
   * @return reference to this, so the API can be used fluently
   */
  public FactoryOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return config;
  }

  /**
   * The JSON object that contains factory configuration entries.
   *
   * @param config - factory config
   * @return reference to this, so the API can be used fluently
   */
  public FactoryOptions setConfig(JsonObject config) {
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
    FactoryOptions that = (FactoryOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config);
  }

  @Override
  public String toString() {
    return "FactoryOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        '}';
  }
}
