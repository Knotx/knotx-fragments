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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@DataObject
public class KnotFlow {

  private String address;

  private Map<String, KnotFlow> onTransition;

  public KnotFlow(String address, Map<String, KnotFlow> onTransition) {
    this.address = address;
    this.onTransition = onTransition;
  }

  public KnotFlow(JsonObject json) {
    fromJson(json, this);
  }

  public String getAddress() {
    return address;
  }

  public KnotFlow get(String transition) {
    return onTransition.get(transition);
  }

  public JsonObject toJson() {
    JsonObject map = new JsonObject();
    onTransition.forEach((key, value) -> map.put(key, value.toJson()));

    return new JsonObject().put("address", address).put("onTransition", map);
  }

  /**
   * TODO fix me later
   * This code comes from Vert.x Converter
   */
  private static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json,
      KnotFlow knotFlow) {
    knotFlow.onTransition = Collections.emptyMap();
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "address":
          if (member.getValue() instanceof String) {
            knotFlow.address = (String) member.getValue();
          }
          break;
        case "onTransition":
          if (member.getValue() instanceof JsonObject) {
            java.util.Map<String, KnotFlow> map = new java.util.LinkedHashMap<>();
            ((Iterable<java.util.Map.Entry<String, Object>>) member.getValue()).forEach(entry -> {
              if (entry.getValue() instanceof JsonObject) {
                map.put(entry.getKey(),
                    new KnotFlow((JsonObject) entry.getValue()));
              }
            });
            knotFlow.onTransition = map;
          }
          break;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KnotFlow knotFlow = (KnotFlow) o;
    return Objects.equals(address, knotFlow.address) &&
        Objects.equals(onTransition, knotFlow.onTransition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, onTransition);
  }

  @Override
  public String toString() {
    return "KnotFlow{" +
        "address='" + address + '\'' +
        ", onTransition=" + onTransition +
        '}';
  }
}
