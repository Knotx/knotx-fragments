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
package io.knotx.fragments.engine;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class OperationMetadata {

  private String factory;

  private JsonObject data;

  public OperationMetadata() {
    // empty
  }

  public OperationMetadata(JsonObject obj) {
    OperationMetadataConverter.fromJson(obj, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    OperationMetadataConverter.toJson(this, json);
    return json;
  }

  public String getFactory() {
    return factory;
  }

  public OperationMetadata setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getData() {
    return data;
  }

  public OperationMetadata setData(JsonObject data) {
    this.data = data;
    return this;
  }

  @Override
  public String toString() {
    return "OperationMetadata{" +
        "factory='" + factory + '\'' +
        ", data=" + data +
        '}';
  }
}
