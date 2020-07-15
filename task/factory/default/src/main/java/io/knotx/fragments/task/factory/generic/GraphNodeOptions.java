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
package io.knotx.fragments.task.factory.generic;

import io.knotx.fragments.task.factory.generic.node.NodeOptions;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeConfig;
import io.knotx.fragments.task.factory.generic.node.action.ActionNodeFactory;
import io.knotx.fragments.task.factory.generic.node.subtasks.SubtasksNodeConfig;
import io.knotx.fragments.task.factory.generic.node.subtasks.SubtasksNodeFactory;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Graph node options model.
 */
@DataObject(generateConverter = true)
public class GraphNodeOptions {

  private NodeOptions node;
  private Map<String, GraphNodeOptions> onTransitions;

  public GraphNodeOptions(NodeOptions nodeOptions, Map<String, GraphNodeOptions> transitions) {
    this.node = nodeOptions;
    this.onTransitions = transitions;
  }

  public GraphNodeOptions(String action, Map<String, GraphNodeOptions> transitions) {
    init();
    setAction(action);
    this.onTransitions = transitions;
  }

  public GraphNodeOptions(List<GraphNodeOptions> subTasks,
      Map<String, GraphNodeOptions> transitions) {
    init();
    setSubtasks(subTasks);
    this.onTransitions = transitions;
  }

  public GraphNodeOptions(JsonObject json) {
    init();
    GraphNodeOptionsConverter.fromJson(json, this);
    if (this.onTransitions == null) {
      this.onTransitions = Collections.emptyMap();
    }
  }

  private void init() {
    node = new NodeOptions();
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    GraphNodeOptionsConverter.toJson(this, result);
    return result;
  }

  public NodeOptions getNode() {
    return node;
  }

  /**
   * Node options define a node factory and its configuration.
   *
   * @param node - node options
   * @return reference to this, so the API can be used fluently
   */
  public GraphNodeOptions setNode(NodeOptions node) {
    this.node = node;
    return this;
  }

  /**
   * It specifies the next graph node for the given transition. If no graph edge is defined, then an
   * empty value is returned.
   *
   * @param transition - non blank transition
   * @return the next node options if defined
   */
  public Optional<GraphNodeOptions> get(String transition) {
    return Optional.ofNullable(onTransitions.get(transition));
  }

  public Map<String, GraphNodeOptions> getOnTransitions() {
    return onTransitions;
  }

  /**
   * The outgoing graph node edges, called transitions. A transition is named graph edge that
   * defines the next graph node in fragment's processing.
   *
   * @param onTransitions - map of possible transitions.
   * @return reference to this, so the API can be used fluently
   */
  public GraphNodeOptions setOnTransitions(Map<String, GraphNodeOptions> onTransitions) {
    this.onTransitions = onTransitions;
    return this;
  }

  /**
   * Alias for "onTransitions".
   * See {@link #setOnTransitions(Map) setOnTransitions}
   *
   * @param on - map of possible transitions
   * @return reference to this, so the API can be used fluently
   */
  public GraphNodeOptions setOn(Map<String, GraphNodeOptions> on) {
    this.onTransitions = on;
    return this;
  }

  /**
   * Sets a node factory name to {@code ActionNodeFactory.NAME} and configures the action.
   *
   * @param action - action name for action node config
   * @return reference to this, so the API can be used fluently
   * @see ActionNodeFactory#NAME
   */
  public GraphNodeOptions setAction(String action) {
    node.setFactory(ActionNodeFactory.NAME);
    node.setConfig(new ActionNodeConfig(action).toJson());
    return this;
  }

  /**
   * Sets a node factory name to {@code SubtasksNodeFactory.NAME} and configures subgraphs.
   *
   * @param subtasks - list of subtasks (subgraphs) options
   * @return reference to this, so the API can be used fluently
   * @see SubtasksNodeFactory#NAME
   * @deprecated use {@link #setSubtasks(List)}
   */
  @Deprecated
  public GraphNodeOptions setActions(List<GraphNodeOptions> subtasks) {
    setSubtasks(subtasks);
    return this;
  }

  /**
   * Sets a node factory name to {@code SubtasksNodeFactory.NAME} and configures subgraphs.
   *
   * @param subtasks - list of subtasks (subgraphs) options
   * @return reference to this, so the API can be used fluently
   * @see SubtasksNodeFactory#NAME
   */
  public GraphNodeOptions setSubtasks(List<GraphNodeOptions> subtasks) {
    node.setFactory(SubtasksNodeFactory.NAME);
    node.setConfig(new SubtasksNodeConfig(subtasks).toJson());
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
    GraphNodeOptions that = (GraphNodeOptions) o;
    return Objects.equals(node, that.node) &&
        Objects.equals(onTransitions, that.onTransitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, onTransitions);
  }

  @Override
  public String toString() {
    return "GraphNodeOptions{" +
        "node=" + node +
        ", onTransitions=" + onTransitions +
        '}';
  }
}
