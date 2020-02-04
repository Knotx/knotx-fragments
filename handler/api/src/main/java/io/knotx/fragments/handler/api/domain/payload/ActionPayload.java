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
public class ActionPayload {

  private static final String RESULT_KEY = "_result";

  private final ActionRequest request;
  private final ActionResponse response;
  private final Object result;

  private ActionPayload(ActionRequest request, ActionResponse response, Object result) {
    this.request = request;
    this.response = response;
    this.result = result;
  }

  public ActionPayload(JsonObject jsonObject) {
    this.request = new ActionRequest(jsonObject.getJsonObject(ActionRequest.KEY));
    this.response = new ActionResponse(jsonObject.getJsonObject(ActionResponse.KEY));
    this.result = jsonObject.getValue(RESULT_KEY);
  }

  public static ActionPayload create(ActionRequest request, ActionResponse response, Object result) {
    return new ActionPayload(request, response, result);
  }

  public static ActionPayload success(ActionRequest request, Object result) {
    return new ActionPayload(request, ActionResponse.success(), result);
  }

  public static ActionPayload error(ActionRequest request, String errorCode, String errorMessage) {
    return new ActionPayload(request, ActionResponse.error(errorCode, errorMessage), null);
  }

  public static ActionPayload noResult(ActionRequest request, ActionResponse response) {
    return new ActionPayload(request, response, null);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(ActionRequest.KEY, request.toJson());
    json.put(ActionResponse.KEY, response.toJson());
    json.put(RESULT_KEY, result);
    return json;
  }

  public ActionRequest getRequest() {
    return request;
  }

  public ActionResponse getResponse() {
    return response;
  }

  public Object getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "ActionPayload{" +
        "request=" + request +
        ", response=" + response +
        ", result=" + result +
        '}';
  }
}

