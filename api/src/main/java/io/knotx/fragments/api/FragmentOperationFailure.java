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

import io.reactivex.exceptions.CompositeException;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class FragmentOperationFailure {

  public static final String GENERAL_EXCEPTION = "_GENERAL_EXCEPTION";

  private String code;
  private String message;
  private List<FragmentOperationException> exceptions;

  public static FragmentOperationFailure newInstance(String code, String message) {
    return new FragmentOperationFailure()
        .setCode(code)
        .setMessage(message)
        .setExceptions(Collections.emptyList());
  }

  public static FragmentOperationFailure newInstance(Throwable error) {
    return new FragmentOperationFailure()
        .setCode(GENERAL_EXCEPTION)
        .setMessage(error.getMessage())
        .setExceptions(flatCompositeExceptions(error));
  }

  public FragmentOperationFailure() {
    // default constructor
  }

  public FragmentOperationFailure(JsonObject json) {
    FragmentOperationFailureConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    FragmentOperationFailureConverter.toJson(this, result);
    return result;
  }

  public String getCode() {
    return code;
  }

  public FragmentOperationFailure setCode(String code) {
    this.code = code;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public FragmentOperationFailure setMessage(String message) {
    this.message = message;
    return this;
  }

  public List<FragmentOperationException> getExceptions() {
    return exceptions;
  }

  public FragmentOperationFailure setExceptions(List<FragmentOperationException> exceptions) {
    this.exceptions = exceptions;
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
    FragmentOperationFailure that = (FragmentOperationFailure) o;
    return Objects.equals(code, that.code) &&
        Objects.equals(message, that.message) &&
        Objects.equals(exceptions, that.exceptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, exceptions);
  }

  @Override
  public String toString() {
    return "FragmentOperationFailure{" +
        "code='" + code + '\'' +
        ", message='" + message + '\'' +
        ", errors=" + exceptions +
        '}';
  }

  private static List<FragmentOperationException> flatCompositeExceptions(Throwable error) {
    List<FragmentOperationException> errors;
    if (error instanceof CompositeException) {
      errors = ((CompositeException) error).getExceptions().stream()
          .map(FragmentOperationException::newInstance).collect(Collectors.toList());
    } else {
      errors = Collections.singletonList(FragmentOperationException.newInstance(error));
    }
    return errors;
  }
}
