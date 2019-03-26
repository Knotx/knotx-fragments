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
package io.knotx.engine.handler.proxy;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import java.util.Optional;

@DataObject
public class OperationProxyFactoryOptions {

  private static final String FACTORY_KEY = "factory";
  private static final String CONFIG_KEY = "config";
  private static final String NEXT_KEY = "next";

  private final String factory;
  private final JsonObject config;
  private final String next;

  public OperationProxyFactoryOptions(String factory, JsonObject config) {
    this(factory, config, null);
  }

  public OperationProxyFactoryOptions(String factory, JsonObject config, String next) {
    this.factory = factory;
    this.config = config;
    this.next = next;
  }

  public OperationProxyFactoryOptions(JsonObject json) {
    this.factory = json.getString(FACTORY_KEY);
    this.config = json.getJsonObject(CONFIG_KEY);
    this.next = json.getString(NEXT_KEY);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(FACTORY_KEY, factory);
    json.put(CONFIG_KEY, config);
    json.put(NEXT_KEY, next);
    return json;
  }

  public String getFactory() {
    return factory;
  }


  public JsonObject getConfig() {
    return config;
  }

  public Optional<String> getNext() {
    return Optional.ofNullable(next);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationProxyFactoryOptions that = (OperationProxyFactoryOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config) &&
        Objects.equals(next, that.next);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config, next);
  }

  @Override
  public String toString() {
    return "OperationProxyFactoryOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        ", next='" + next + '\'' +
        '}';
  }
}
