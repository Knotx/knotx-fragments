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

public class NodeExecutionData {

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

  static class Response {

    private final String transition;
    private final JsonArray invocations;

    Response(String transition, JsonArray invocations) {
      this.transition = transition;
      this.invocations = invocations;
    }

    public String getTransition() {
      return transition;
    }

    public JsonArray getInvocations() {
      return invocations;
    }
  }

}
