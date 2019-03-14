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
package io.knotx.engine.api;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class KnotFlowStep {

  private String address;

  private DeliveryOptions deliveryOptions;

  private CircuitBreakerOptions circuitBreakerOptions;

  public KnotFlowStep(String address) {
    this(address, new DeliveryOptions());
  }

  public KnotFlowStep(String address, DeliveryOptions deliveryOptions) {
    this(address, deliveryOptions, null);
  }

  public KnotFlowStep(String address, DeliveryOptions deliveryOptions,
      CircuitBreakerOptions circuitBreakerOptions) {
    if (StringUtils.isBlank(address)) {
      throw new IllegalStateException("Event bus address in Knot flow can not be empty!");
    }
    this.address = address;
    this.deliveryOptions = deliveryOptions;
    this.circuitBreakerOptions = circuitBreakerOptions;
  }

  public KnotFlowStep(JsonObject json) {
    KnotFlowStepConverter.fromJson(json, this);
    if (this.deliveryOptions == null) {
      this.deliveryOptions = new DeliveryOptions();
    }
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    KnotFlowStepConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public String getAddress() {
    return address;
  }

  public KnotFlowStep setAddress(String address) {
    this.address = address;
    return this;
  }

  public DeliveryOptions getDeliveryOptions() {
    return deliveryOptions;
  }

  public KnotFlowStep setDeliveryOptions(
      DeliveryOptions deliveryOptions) {
    this.deliveryOptions = deliveryOptions;
    return this;
  }

  public CircuitBreakerOptions getCircuitBreakerOptions() {
    return circuitBreakerOptions;
  }

  public KnotFlowStep setCircuitBreakerOptions(
      CircuitBreakerOptions circuitBreakerOptions) {
    this.circuitBreakerOptions = circuitBreakerOptions;
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
    KnotFlowStep that = (KnotFlowStep) o;
    return Objects.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, deliveryOptions, circuitBreakerOptions);
  }

  @Override
  public String toString() {
    return "KnotFlowStep{" +
        "address='" + address + '\'' +
        ", deliveryOptions=" + deliveryOptions +
        ", circuitBreakerOptions=" + circuitBreakerOptions +
        '}';
  }
}
