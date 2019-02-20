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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.handler;

import io.knotx.engine.api.KnotFlow;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * It is the named Knot Flow.
 */
@DataObject(generateConverter = true)
public class KnotFlowContext {

  private String name;

  private KnotFlow knotFlow;

  public KnotFlowContext(String name, KnotFlow knotFlow) {
    this.name = name;
    this.knotFlow = knotFlow;
  }

  public KnotFlowContext(JsonObject json) {
    KnotFlowContextConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    KnotFlowContextConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public String getName() {
    return name;
  }

  public KnotFlowContext setName(String name) {
    this.name = name;
    return this;
  }

  public KnotFlow getKnotFlow() {
    return knotFlow;
  }

  public KnotFlowContext setKnotFlow(KnotFlow knotFlow) {
    this.knotFlow = knotFlow;
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
    KnotFlowContext that = (KnotFlowContext) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(knotFlow, that.knotFlow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, knotFlow);
  }

  @Override
  public String toString() {
    return "KnotFlowContext{" +
        "name='" + name + '\'' +
        ", knotFlow=" + knotFlow +
        '}';
  }
}
