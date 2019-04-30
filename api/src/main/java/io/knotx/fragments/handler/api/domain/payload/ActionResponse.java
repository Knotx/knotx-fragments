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
package io.knotx.fragments.handler.api.domain.payload;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class ActionResponse {

  static final String KEY = "_response";
  private static final String SUCCESS_KEY = "success";
  private static final String METADATA_KEY = "metadata";

  private boolean success;
  private ActionResponseError error;
  private JsonObject metadata;

  ActionResponse() {
    // hidden
    metadata = new JsonObject();
  }

  public ActionResponse(JsonObject json) {
    this.success = json.getBoolean(SUCCESS_KEY);
    if (json.containsKey(ActionResponseError.KEY)) {
      this.error = new ActionResponseError(json.getJsonObject(ActionResponseError.KEY));
    }
    this.metadata = json.getJsonObject(METADATA_KEY);
  }

  public static ActionResponse success() {
    ActionResponse response = new ActionResponse();
    response.success = true;
    return response;
  }

  public static ActionResponse error(String errorCode) {
    return error(errorCode, null);
  }

  public static ActionResponse error(String errorCode, String errorMessage) {
    ActionResponse response = new ActionResponse();
    response.success = false;
    response.error = new ActionResponseError(errorCode, errorMessage);
    return response;
  }

  public ActionResponse appendMetadata(String key, Object value) {
    metadata.put(key, value);
    return this;
  }

  public boolean isSuccess() {
    return success;
  }

  public ActionResponseError getError() {
    return error;
  }

  public JsonObject getMetadata() {
    return metadata;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(SUCCESS_KEY, success);
    if (error != null) {
      json.put(ActionResponseError.KEY, error.toJson());
    }
    json.put(METADATA_KEY, metadata);
    return json;
  }

  @Override
  public String toString() {
    return "ActionResponse{" +
        "success=" + success +
        ", error=" + error +
        ", metadata=" + metadata +
        '}';
  }
}