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
package io.knotx.fragments.task.handler.log.api.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * Node response details.
 */
@DataObject(generateConverter = true)
public class GraphNodeResponseLog {

  private String transition;
  private JsonObject log;
  private List<GraphNodeErrorLog> errors;

  public static GraphNodeResponseLog newInstance(String transition, JsonObject log) {
    return new GraphNodeResponseLog()
        .setTransition(transition)
        .setLog(log);
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
   * Node response log.
   *
   * @return node response log
   */
  public JsonObject getLog() {
    return log;
  }

  public GraphNodeResponseLog setLog(JsonObject log) {
    this.log = log;
    return this;
  }

  public List<GraphNodeErrorLog> getErrors() {
    return errors;
  }

  public GraphNodeResponseLog setErrors(List<GraphNodeErrorLog> errors) {
    this.errors = errors;
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
        Objects.equals(log, that.log) &&
        Objects.equals(errors, that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transition, log, errors);
  }

  @Override
  public String toString() {
    return "GraphNodeResponseLog{" +
        "transition='" + transition + '\'' +
        ", log=" + log +
        ", errors=" + errors +
        '}';
  }
}
