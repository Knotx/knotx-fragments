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
package io.knotx.fragments.handler.options;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@DataObject(generateConverter = true)
public class GraphOptions {

  private String proxy;

  private Map<String, GraphOptions> transitions;

  public GraphOptions(String proxyAlias, Map<String, GraphOptions> transitions) {
    if (proxyAlias == null) {
      throw new IllegalStateException("Proxy can not be null");
    }
    this.proxy = proxyAlias;
    this.transitions = transitions;
  }

  public GraphOptions(JsonObject json) {
    GraphOptionsConverter.fromJson(json, this);
    if (this.transitions == null) {
      this.transitions = Collections.emptyMap();
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    GraphOptionsConverter.toJson(this, result);
    return result;
  }

  public String getProxy() {
    return proxy;
  }

  public GraphOptions setProxy(String proxy) {
    this.proxy = proxy;
    return this;
  }

  public Optional<GraphOptions> get(String transition) {
    return Optional.ofNullable(transitions.get(transition));
  }

  public Map<String, GraphOptions> getTransitions() {
    return transitions;
  }

  public GraphOptions setTransitions(
      Map<String, GraphOptions> transitions) {
    this.transitions = transitions;
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
    GraphOptions that = (GraphOptions) o;
    return Objects.equals(proxy, that.proxy) &&
        Objects.equals(transitions, that.transitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proxy, transitions);
  }

  @Override
  public String toString() {
    return "GraphOptions{" +
        "proxy='" + proxy + '\'' +
        ", transitions=" + transitions +
        '}';
  }
}
