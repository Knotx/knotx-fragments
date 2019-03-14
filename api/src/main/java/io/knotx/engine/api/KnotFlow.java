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

@DataObject(generateConverter = true)
public class KnotFlow {

  private KnotFlowStep step;

  private Map<String, KnotFlow> onTransition;

  public KnotFlow(String address, Map<String, KnotFlow> onTransition) {
    this.step = new KnotFlowStep(address);
    this.onTransition = onTransition;
  }

  public KnotFlow(KnotFlowStep stepOptions, Map<String, KnotFlow> onTransition) {
    if (stepOptions == null) {
      throw new IllegalStateException("Step options can not be null");
    }
    this.step = stepOptions;
    this.onTransition = onTransition;
  }

  public KnotFlow(JsonObject json) {
    KnotFlowConverter.fromJson(json, this);
    if (this.onTransition == null) {
      this.onTransition = Collections.emptyMap();
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    KnotFlowConverter.toJson(this, result);
    return result;
  }

  public KnotFlowStep getStep() {
    return step;
  }

  public KnotFlow setStep(KnotFlowStep step) {
    this.step = step;
    return this;
  }

  public KnotFlow get(String transition) {
    return onTransition.get(transition);
  }

  public Map<String, KnotFlow> getOnTransition() {
    return onTransition;
  }

  public KnotFlow setOnTransition(
      Map<String, KnotFlow> onTransition) {
    this.onTransition = onTransition;
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
    KnotFlow knotFlow = (KnotFlow) o;
    return Objects.equals(step, knotFlow.step) &&
        Objects.equals(onTransition, knotFlow.onTransition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(step, onTransition);
  }


  @Override
  public String toString() {
    return "KnotFlow{" +
        "step=" + step +
        ", onTransition=" + onTransition +
        '}';
  }
}
