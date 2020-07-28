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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject(generateConverter = true)
public class GraphNodeErrorLog {

  private String className;
  private String message;
  private JsonArray stacktrace;

  public GraphNodeErrorLog() {
    // default constructor
  }

  public GraphNodeErrorLog(JsonObject json) {
    GraphNodeErrorLogConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    GraphNodeErrorLogConverter.toJson(this, result);
    return result;
  }

  public String getClassName() {
    return className;
  }

  public GraphNodeErrorLog setClassName(String className) {
    this.className = className;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public GraphNodeErrorLog setMessage(String message) {
    this.message = message;
    return this;
  }

  public JsonArray getStacktrace() {
    return stacktrace;
  }

  public GraphNodeErrorLog setStacktrace(JsonArray stacktrace) {
    this.stacktrace = stacktrace;
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
    GraphNodeErrorLog that = (GraphNodeErrorLog) o;
    return Objects.equals(className, that.className) &&
        Objects.equals(message, that.message) &&
        Objects.equals(stacktrace, that.stacktrace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, message, stacktrace);
  }

  @Override
  public String toString() {
    return "GraphNodeErrorLog{" +
        "className='" + className + '\'' +
        ", message='" + message + '\'' +
        ", stacktrace=" + stacktrace +
        '}';
  }
}
