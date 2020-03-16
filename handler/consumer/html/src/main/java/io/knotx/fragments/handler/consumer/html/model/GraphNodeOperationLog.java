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
package io.knotx.fragments.handler.consumer.html.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * It represents node metadata.
 */
@DataObject(generateConverter = true)
public class GraphNodeOperationLog {

  private String factory;

  private JsonObject data;

  public static GraphNodeOperationLog empty() {
    return new GraphNodeOperationLog().setFactory(StringUtils.EMPTY).setData(new JsonObject());
  }

  public static GraphNodeOperationLog newInstance(String factory, JsonObject data) {
    return new GraphNodeOperationLog().setFactory(factory).setData(data);
  }

  public GraphNodeOperationLog() {
    // default constructor
  }

  public GraphNodeOperationLog(JsonObject obj) {
    GraphNodeOperationLogConverter.fromJson(obj, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    GraphNodeOperationLogConverter.toJson(this, json);
    return json;
  }

  /**
   * Node factory name.
   *
   * @return node factory
   */
  public String getFactory() {
    return factory;
  }

  public GraphNodeOperationLog setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  /**
   * Unstructured node factory metadata.
   *
   * @return node factory metadata
   */
  public JsonObject getData() {
    return data;
  }

  public GraphNodeOperationLog setData(JsonObject data) {
    this.data = data;
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
    GraphNodeOperationLog that = (GraphNodeOperationLog) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, data);
  }

  @Override
  public String toString() {
    return "OperationMetadata{" +
        "factory='" + factory + '\'' +
        ", data=" + data +
        '}';
  }
}
