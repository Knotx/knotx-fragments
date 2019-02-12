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
package io.knotx.engine.api;

import io.knotx.server.api.context.ClientRequest;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject
public class FragmentContext {

  private static final String FRAGMENT_EVENT = "fragmentEvent";
  private static final String CLIENT_REQUEST = "clientRequest";

  private final FragmentEvent fragmentEvent;
  private final ClientRequest clientRequest;

  public FragmentContext(FragmentEvent fragmentEvent,
      ClientRequest clientRequest) {
    this.fragmentEvent = fragmentEvent;
    this.clientRequest = clientRequest;
  }

  public FragmentContext(JsonObject json) {
    this.fragmentEvent = new FragmentEvent(json.getJsonObject(FRAGMENT_EVENT));
    this.clientRequest = new ClientRequest(json.getJsonObject(CLIENT_REQUEST));
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_EVENT, fragmentEvent.toJson())
        .put(CLIENT_REQUEST, clientRequest.toJson());
  }

  public FragmentEvent getFragmentEvent() {
    return fragmentEvent;
  }

  public ClientRequest getClientRequest() {
    return clientRequest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FragmentContext that = (FragmentContext) o;
    return Objects.equals(fragmentEvent, that.fragmentEvent) &&
        Objects.equals(clientRequest, that.clientRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragmentEvent, clientRequest);
  }

  @Override
  public String toString() {
    return "FragmentContext{" +
        "fragmentEvent=" + fragmentEvent +
        ", clientRequest=" + clientRequest +
        '}';
  }
}
