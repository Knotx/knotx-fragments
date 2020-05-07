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
package io.knotx.fragments.handler.consumer.api.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Node response details.
 */
@DataObject(generateConverter = true)
public class GraphNodeResponseLog {

  private String transition;
  private JsonArray invocations;

  public static GraphNodeResponseLog newInstance(String transition, JsonArray invocations) {
    return new GraphNodeResponseLog()
        .setTransition(transition)
        .setInvocations(invocations);
  }

  public GraphNodeResponseLog() {
    // default constructor
  }

  public GraphNodeResponseLog(JsonObject json) {
    // default constructor
    GraphNodeResponseLogConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    GraphNodeResponseLogConverter.toJson(this, result);
    return result;
  }

  /**
   * Node response transition.
   *
   * @return node response transition.
   */
  public String getTransition() {
    return transition;
  }

  public GraphNodeResponseLog setTransition(String transition) {
    this.transition = transition;
    return this;
  }

  /**
   * List of node invocation(s) logs. See <a href="https://github.com/Knotx/knotx-fragments/tree/master/engine#node-log">node
   * log</a> for more details.
   */
  public JsonArray getInvocations() {
    return invocations;
  }

  public GraphNodeResponseLog setInvocations(JsonArray invocations) {
    this.invocations = invocations;
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
    GraphNodeResponseLog that = (GraphNodeResponseLog) o;
    return Objects.equals(transition, that.transition) &&
        Objects.equals(invocations, that.invocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transition, invocations);
  }

  @Override
  public String toString() {
    return "GraphNodeResponseLog{" +
        "transition='" + transition + '\'' +
        ", invocations=" + invocations +
        '}';
  }
}
