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
package io.knotx.fragments.action.library.http.payload;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class ActionResponseError {

  static final String KEY = "error";
  private static final String CODE_KEY = "code";
  private static final String MESSAGE_KEY = "message";

  private String code;
  private String message;

  public ActionResponseError(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public ActionResponseError(JsonObject json) {
    this.code = json.getString(CODE_KEY);
    this.message = json.getString(MESSAGE_KEY);
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(CODE_KEY, code);
    json.put(MESSAGE_KEY, message);
    return json;
  }

  @Override
  public String toString() {
    return "ActionResponseError{" +
        "code='" + code + '\'' +
        ", message='" + message + '\'' +
        '}';
  }
}