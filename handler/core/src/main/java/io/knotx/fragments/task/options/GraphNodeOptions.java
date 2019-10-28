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
package io.knotx.fragments.task.options;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * It is {@link io.knotx.fragments.engine.Task} processing configuration. Task is graph of nodes (in
 * fact it is tree structure). It defines {@link NodeOptions} and outgoing directed graph edges,
 * called {@code Transitions}.
 *
 * It represents JSON configuration:
 * <pre>
 * {
 *   node = {
 *     factory = action
 *     config {
 *       action = a
 *     }
 *   }
 *   onTransitions {
 *     _success {
 *       node = {
 *         factory = action
 *         config {
 *           action = b
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * Please note that Transitions define next graph nodes.
 */
@DataObject(generateConverter = true)
public class GraphNodeOptions {

  // TODO move to node factories
  public static final String SUBTASKS = "subtasks";
  public static final String ACTION = "action";

  private NodeOptions node;
  private Map<String, GraphNodeOptions> onTransitions;

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

  /**
   * Gets node options.
   *
   * @return node options
   */
  public NodeOptions getNode() {
    return node;
  }

  /**
   * Sets node options defining node factory and its configuration.
   *
   * @param node node options
   * @return reference to this, so the API can be used fluently
   */
  public GraphNodeOptions setNode(NodeOptions node) {
    this.node = node;
    return this;
  }

  /**
   * Gets next graph node for given Transition. If Transition is not configured then {@link
   * Optional#empty()} is returned.
   *
   * @param transition transition
   */
  public Optional<GraphNodeOptions> get(String transition) {
    return Optional.ofNullable(onTransitions.get(transition));
  }

  /**
   * Gets Transition to next graph node map.
   *
   * @return Transition to graph node map
   */
  public Map<String, GraphNodeOptions> getOnTransitions() {
    return onTransitions;
  }

  /**
   * Sets outgoing graph node edges, called {@code Transitions}. Transition is String, {@code
   * onTransitions} map links Transition with next graph node.
   *
   * @param onTransitions map of possible transitions.
   * @return reference to this, so the API can be used fluently
   */
  public GraphNodeOptions setOnTransitions(Map<String, GraphNodeOptions> onTransitions) {
    this.onTransitions = onTransitions;
    return this;
  }

  /**
   * @see ActionNodeConfigOptions#setAction(String)
   */
  public GraphNodeOptions setAction(String actionName) {
    node.setFactory(ACTION);
    node.setConfig(new ActionNodeConfigOptions(actionName).toJson());
    return this;
  }

  /**
   * @see SubtasksNodeConfigOptions#setSubtasks(List)
   */
  @Deprecated
  public GraphNodeOptions setActions(List<GraphNodeOptions> subTasks) {
    setSubtasks(subTasks);
    return this;
  }

  /**
   * @see SubtasksNodeConfigOptions#setSubtasks(List)
   */
  public GraphNodeOptions setSubtasks(List<GraphNodeOptions> subtasks) {
    node.setFactory(SUBTASKS);
    node.setConfig(new SubtasksNodeConfigOptions(subtasks).toJson());
    return this;
  }

  // TODO remove when node factories finished
  public boolean isComposite() {
    return SUBTASKS.equals(node.getFactory());
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
