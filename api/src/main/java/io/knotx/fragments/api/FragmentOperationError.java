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
package io.knotx.fragments.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * It wraps an exception from {@link FragmentOperation#apply(FragmentContext, Handler)} and
 * serialize it details to {@link JsonObject}.
 */
@DataObject(generateConverter = true)
public class FragmentOperationError {

  private String className;
  private String message;
  private List<String> stacktrace;

  /**
   * Creates the new instance.
   *
   * @param error exception to serialize
   */
  public static FragmentOperationError newInstance(Throwable error) {
    final List<String> stackTraceLogs = Arrays.stream(error.getStackTrace())
        .map(StackTraceElement::toString)
        .collect(Collectors.toList());

    return new FragmentOperationError()
        .setClassName(error.getClass().getCanonicalName())
        .setMessage(error.getMessage())
        .setStacktrace(stackTraceLogs);
  }

  public FragmentOperationError() {
  }

  public FragmentOperationError(JsonObject json) {
    FragmentOperationErrorConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    FragmentOperationErrorConverter.toJson(this, result);
    return result;
  }

  /**
   * Gets an exception class name.
   *
   * @return class name
   */
  public String getClassName() {
    return className;
  }

  /**
   * Gets an exception details message.
   *
   * @return details message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets a list of serialized stactrace lines.
   *
   * @return stacktrace
   */
  public List<String> getStacktrace() {
    return stacktrace;
  }

  public FragmentOperationError setClassName(String className) {
    this.className = className;
    return this;
  }

  public FragmentOperationError setMessage(String message) {
    this.message = message;
    return this;
  }

  public FragmentOperationError setStacktrace(List<String> stacktrace) {
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
    FragmentOperationError that = (FragmentOperationError) o;
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
    return "FragmentOperationException{" +
        "className='" + className + '\'' +
        ", message='" + message + '\'' +
        ", stacktrace=" + stacktrace +
        '}';
  }
}