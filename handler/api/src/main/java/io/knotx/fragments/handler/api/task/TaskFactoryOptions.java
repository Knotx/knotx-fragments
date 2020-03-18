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
package io.knotx.fragments.handler.api.task;

import io.knotx.fragments.handler.api.exception.ConfigurationException;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Task Factory options model. It specifies task factory by its name and provides task factory
 * config.
 */
@DataObject(generateConverter = true)
public class TaskFactoryOptions {

  private String factory;
  private JsonObject config;

  public TaskFactoryOptions(JsonObject json) {
    TaskFactoryOptionsConverter.fromJson(json, this);
    if (StringUtils.isBlank(factory)) {
      throw new ConfigurationException("Task factory name empty [" + json.toString() + "]");
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    TaskFactoryOptionsConverter.toJson(this, json);
    return json;
  }

  public String getFactory() {
    return factory;
  }

  /**
   * The task factory name that identifies {@code TaskFactory} implementation.
   *
   * @param factory - task factory name
   * @return reference to this, so the API can be used fluently
   */
  public TaskFactoryOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return config;
  }

  /**
   * The JSON object that contains task factory configuration entries.
   *
   * @param config - task factory config
   * @return reference to this, so the API can be used fluently
   */
  public TaskFactoryOptions setConfig(JsonObject config) {
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
    TaskFactoryOptions that = (TaskFactoryOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config);
  }

  @Override
  public String toString() {
    return "TaskFactoryOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        '}';
  }
}
