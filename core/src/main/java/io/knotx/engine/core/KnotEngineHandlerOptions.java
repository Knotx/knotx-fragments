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
package io.knotx.engine.core;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;

@DataObject(generateConverter = true)
public class KnotEngineHandlerOptions {

  private List<KnotFlowContext> flows;

  private DeliveryOptions deliveryOptions;

  public KnotEngineHandlerOptions(List<KnotFlowContext> flows,
      DeliveryOptions deliveryOptions) {
    this.flows = flows;
    this.deliveryOptions = deliveryOptions;
  }

  public KnotEngineHandlerOptions(JsonObject json) {
    KnotEngineHandlerOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    KnotEngineHandlerOptionsConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public DeliveryOptions getDeliveryOptions() {
    return deliveryOptions;
  }

  public KnotEngineHandlerOptions setDeliveryOptions(DeliveryOptions deliveryOptions) {
    this.deliveryOptions = deliveryOptions;
    return this;
  }

  public List<KnotFlowContext> getFlows() {
    return flows;
  }

  public KnotEngineHandlerOptions setFlows(
      List<KnotFlowContext> flows) {
    this.flows = flows;
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
    KnotEngineHandlerOptions that = (KnotEngineHandlerOptions) o;
    return Objects.equals(flows, that.flows) &&
        Objects.equals(deliveryOptions, that.deliveryOptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flows, deliveryOptions);
  }

  @Override
  public String toString() {
    return "KnotEngineHandlerOptions{" +
        "flows=" + flows +
        ", deliveryOptions=" + deliveryOptions +
        '}';
  }
}
