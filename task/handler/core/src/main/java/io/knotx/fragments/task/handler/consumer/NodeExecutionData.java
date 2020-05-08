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
package io.knotx.fragments.task.handler.consumer;

import io.knotx.fragments.task.handler.log.api.model.LoggedNodeStatus;
import io.vertx.core.json.JsonArray;

class NodeExecutionData {

  private final LoggedNodeStatus status;
  private Response response;
  private long started;
  private long finished;

  NodeExecutionData(LoggedNodeStatus status) {
    this.status = status;
  }

  LoggedNodeStatus getStatus() {
    return status;
  }

  public long getStarted() {
    return started;
  }

  public void setStarted(long started) {
    this.started = started;
  }

  public long getFinished() {
    return finished;
  }

  public void setFinished(long finished) {
    this.finished = finished;
  }

  Response getResponse() {
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

    String getTransition() {
      return transition;
    }

    JsonArray getInvocations() {
      return invocations;
    }
  }

}
