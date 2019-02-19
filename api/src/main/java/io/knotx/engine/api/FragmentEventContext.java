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
public class FragmentEventContext {

  private static final String FRAGMENT_EVENT_KEY = "fragmentEvent";
  private static final String CLIENT_REQUEST_KEY = "clientRequest";
  private static final String ORDER_KEY = "order";

  private final FragmentEvent fragmentEvent;
  private final ClientRequest clientRequest;
  private final int order;

  public FragmentEventContext(FragmentEvent fragmentEvent, ClientRequest clientRequest, int order) {
    this.fragmentEvent = fragmentEvent;
    this.clientRequest = clientRequest;
    this.order = order;
  }

  public FragmentEventContext(JsonObject json) {
    this.fragmentEvent = new FragmentEvent(json.getJsonObject(FRAGMENT_EVENT_KEY));
    this.clientRequest = new ClientRequest(json.getJsonObject(CLIENT_REQUEST_KEY));
    this.order = json.getInteger(ORDER_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_EVENT_KEY, fragmentEvent.toJson())
        .put(CLIENT_REQUEST_KEY, clientRequest.toJson())
        .put(ORDER_KEY, order);
  }

  public FragmentEvent getFragmentEvent() {
    return fragmentEvent;
  }

  public ClientRequest getClientRequest() {
    return clientRequest;
  }

  public int getOrder() {
    return order;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FragmentEventContext that = (FragmentEventContext) o;
    return order == that.order &&
        Objects.equals(fragmentEvent, that.fragmentEvent) &&
        Objects.equals(clientRequest, that.clientRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragmentEvent, clientRequest, order);
  }

  @Override
  public String toString() {
    return "FragmentEventContext{" +
        "fragmentEvent=" + fragmentEvent +
        ", clientRequest=" + clientRequest +
        ", order=" + order +
        '}';
  }
}
