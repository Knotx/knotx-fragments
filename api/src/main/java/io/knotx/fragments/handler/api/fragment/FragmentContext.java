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
package io.knotx.fragments.handler.api.fragment;

import io.knotx.fragment.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * This data structure is passed between {@code Actions} that are vertices of a graph.
 */
@DataObject
public class FragmentContext {

  private static final String FRAGMENT_KEY = "fragment";
  private static final String CLIENT_REQUEST_KEY = "clientRequest";

  private final Fragment fragment;
  private final ClientRequest clientRequest;

  public FragmentContext(Fragment fragment, ClientRequest clientRequest) {
    this.fragment = fragment;
    this.clientRequest = clientRequest;
  }

  public FragmentContext(JsonObject json) {
    this.fragment = new Fragment(json.getJsonObject(FRAGMENT_KEY));
    this.clientRequest = new ClientRequest(json.getJsonObject(CLIENT_REQUEST_KEY));
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_KEY, fragment.toJson())
        .put(CLIENT_REQUEST_KEY, clientRequest.toJson());
  }

  /**
   * Fragment that is passed between Actions. It might be transformend or updated by any {@code
   * Action}.
   *
   * @return a Fragment
   */
  public Fragment getFragment() {
    return fragment;
  }

  /**
   * Original {@code ClientRequest}. This property is immutable for the Fragments graph processing.
   *
   * @return client request
   */
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
    return Objects.equals(fragment, that.fragment) &&
        Objects.equals(clientRequest, that.clientRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragment, clientRequest);
  }

  @Override
  public String toString() {
    return "FragmentContext{" +
        "fragment=" + fragment +
        ", clientRequest=" + clientRequest +
        '}';
  }
}
