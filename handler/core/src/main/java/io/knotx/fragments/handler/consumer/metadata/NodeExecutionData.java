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
package io.knotx.fragments.handler.consumer.metadata;

import io.knotx.fragments.handler.LoggedNodeStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class NodeExecutionData {

  private static final String STATUS = "status";
  private static final String RESPONSE = "response";

  private final LoggedNodeStatus status;
  private Response response;

  NodeExecutionData(LoggedNodeStatus status) {
    this.status = status;
  }

  public LoggedNodeStatus getStatus() {
    return status;
  }

  public Response getResponse() {
    return response;
  }

  void setResponse(String transaction, JsonArray invocations) {
    this.response = new Response(transaction, invocations);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject().put(STATUS, status);
    if (response != null) {
      result.put(RESPONSE, response.toJson());
    }
    return result;
  }

  static class Response {

    private static final String TRANSITION = "transition";
    private static final String INVOCATIONS = "invocations";

    private final String transaction;
    private final JsonArray invocations;

    Response(String transaction, JsonArray invocations) {
      this.transaction = transaction;
      this.invocations = invocations;
    }

    String getTransaction() {
      return transaction;
    }

    JsonArray getInvocations() {
      return invocations;
    }

    JsonObject toJson() {
      return new JsonObject()
          .put(TRANSITION, transaction)
          .put(INVOCATIONS, invocations);
    }
  }

}
