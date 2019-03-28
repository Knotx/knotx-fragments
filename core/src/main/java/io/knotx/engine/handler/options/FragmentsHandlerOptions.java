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
package io.knotx.engine.handler.options;

import io.knotx.engine.handler.action.ActionFactoryOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Objects;

@DataObject(generateConverter = true)
public class FragmentsHandlerOptions {

  private Map<String, GraphOptions> flows;

  private Map<String, ActionFactoryOptions> operations;

  public FragmentsHandlerOptions(Map<String, GraphOptions> flows) {
    this.flows = flows;
  }

  public FragmentsHandlerOptions(JsonObject json) {
    FragmentsHandlerOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    FragmentsHandlerOptionsConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public Map<String, GraphOptions> getFlows() {
    return flows;
  }

  public FragmentsHandlerOptions setFlows(
      Map<String, GraphOptions> flows) {
    this.flows = flows;
    return this;
  }

  public Map<String, ActionFactoryOptions> getOperations() {
    return operations;
  }

  public FragmentsHandlerOptions setOperations(
      Map<String, ActionFactoryOptions> operations) {
    this.operations = operations;
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
    FragmentsHandlerOptions that = (FragmentsHandlerOptions) o;
    return Objects.equals(flows, that.flows) &&
        Objects.equals(operations, that.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flows, operations);
  }

  @Override
  public String toString() {
    return "FragmentsHandlerOptions{" +
        "flows=" + flows +
        ", operations=" + operations +
        '}';
  }
}
