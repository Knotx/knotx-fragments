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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines graph's verticle with outgoing directed edges ({@code Transitions}).
 */
@DataObject(generateConverter = true)
public class NodeOptions {

  private String action;

  private List<NodeOptions> actions;

  private Map<String, NodeOptions> onTransitions;

  public NodeOptions(String action, Map<String, NodeOptions> transitions) {
    if (action == null) {
      throw new IllegalStateException("Proxy can not be null");
    }
    this.action = action;
    this.onTransitions = transitions;
  }

  public NodeOptions(List<NodeOptions> actions, Map<String, NodeOptions> transitions) {
    if (actions == null) {
      throw new IllegalStateException("No actions defined");
    }
    this.actions = actions;
    this.onTransitions = transitions;
  }

  public NodeOptions(JsonObject json) {
    NodeOptionsConverter.fromJson(json, this);
    if (this.onTransitions == null) {
      this.onTransitions = Collections.emptyMap();
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    NodeOptionsConverter.toJson(this, result);
    return result;
  }

  public String getAction() {
    return action;
  }

  /**
   * Sets {@code Action} name. This action will be executed during processing given graph node.
   *
   * @param action action name
   * @return reference to this, so the API can be used fluently
   */
  public NodeOptions setAction(String action) {
    this.action = action;
    return this;
  }

  public List<NodeOptions> getActions() {
    return actions;
  }

  /**
   * Sets list of {@code NodeOptions} that represents {@code Actions} that will be executed in parallel.
   *
   * @param actions list of {@link NodeOptions}
   * @return reference to this, so the API can be used fluently
   */
  public NodeOptions setActions(List<NodeOptions> actions) {
    this.actions = actions;
    return this;
  }

  public Optional<NodeOptions> get(String transition) {
    return Optional.ofNullable(onTransitions.get(transition));
  }

  public Map<String, NodeOptions> getOnTransitions() {
    return onTransitions;
  }

  /**
   * Sets the {@code Map} of possible onTransitions for the given graph node.
   *
   * @param onTransitions map of possible transitions.
   * @return reference to this, so the API can be used fluently
   */
  public NodeOptions setOnTransitions(
      Map<String, NodeOptions> onTransitions) {
    this.onTransitions = onTransitions;
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
    NodeOptions that = (NodeOptions) o;
    return Objects.equals(action, that.action) &&
        Objects.equals(actions, that.actions) &&
        Objects.equals(onTransitions, that.onTransitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, actions, onTransitions);
  }

  @Override
  public String toString() {
    return "NodeOptions{" +
        "action='" + action + '\'' +
        ", actions=" + actions +
        ", onTransitions=" + onTransitions +
        '}';
  }
}
