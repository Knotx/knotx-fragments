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
package io.knotx.fragments.supplier.single;

import static io.knotx.fragments.api.Fragment.JSON_OBJECT_TYPE;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Describes a single Fragment that is created by the supplier.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class SingleFragmentSupplierOptions {

  private String type;
  private JsonObject configuration;
  private String body;
  private JsonObject payload;

  public SingleFragmentSupplierOptions(JsonObject json) {
    init();
    if (json != null) {
      SingleFragmentSupplierOptionsConverter.fromJson(json, this);
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SingleFragmentSupplierOptionsConverter.toJson(this, json);
    return json;
  }

  private void init() {
    this.type = JSON_OBJECT_TYPE;
    this.configuration = new JsonObject();
    this.body = "{}";
    this.payload = new JsonObject();
  }

  public String getType() {
    return type;
  }

  /**
   * Sets the initial type of the Fragment. By default it is an empty string.
   *
   * @param type the type of the Fragment
   * @return reference to this, so the API can be used fluently
   */
  public SingleFragmentSupplierOptions setType(String type) {
    this.type = type;
    return this;
  }

  public JsonObject getConfiguration() {
    return configuration;
  }

  /**
   * Sets the initial configuration of the Fragment. By default it is an empty Json Object.
   *
   * @param configuration the configuration of the Fragment
   * @return reference to this, so the API can be used fluently
   */
  public SingleFragmentSupplierOptions setConfiguration(JsonObject configuration) {
    this.configuration = configuration;
    return this;
  }

  public String getBody() {
    return body;
  }

  /**
   * Sets the initial body of the Fragment. By default it is an empty string.
   *
   * @param body the body of the Fragment
   * @return reference to this, so the API can be used fluently
   */
  public SingleFragmentSupplierOptions setBody(String body) {
    this.body = body;
    return this;
  }

  public JsonObject getPayload() {
    return payload;
  }

  /**
   * Sets the initial payload of the Fragment. By default it is an empty Json Object.
   *
   * @param payload the payload of the Fragment
   * @return reference to this, so the API can be used fluently
   */
  public SingleFragmentSupplierOptions setPayload(JsonObject payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public String toString() {
    return "SingleFragmentSupplierOptions{" +
        "type='" + type + '\'' +
        ", configuration=" + configuration +
        ", body='" + body + '\'' +
        ", payload=" + payload +
        '}';
  }
}
