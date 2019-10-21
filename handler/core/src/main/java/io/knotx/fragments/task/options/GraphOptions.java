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

import io.knotx.fragments.task.node.options.ActionNodeConfigOptions;
import io.knotx.fragments.task.node.options.NodeOptions;
import io.knotx.fragments.task.node.options.SubTasksNodeConfigOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Defines graph's verticle with outgoing directed edges ({@code Transitions}).
 */
@DataObject(generateConverter = true)
public class GraphOptions {

  // TODO move to node factories
  public static final String SUBTASKS = "subTasks";
  public static final String ACTION = "action";

  private NodeOptions node;
  private Map<String, GraphOptions> onTransitions;

  public GraphOptions(JsonObject json) {
    init();
    GraphOptionsConverter.fromJson(json, this);
    if (this.onTransitions == null) {
      this.onTransitions = Collections.emptyMap();
    }
  }

  private void init() {
    node = new NodeOptions();
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    GraphOptionsConverter.toJson(this, result);
    return result;
  }

  public NodeOptions getNode() {
    return node;
  }

  public void setNode(NodeOptions node) {
    this.node = node;
  }

  public Optional<GraphOptions> get(String transition) {
    return Optional.ofNullable(onTransitions.get(transition));
  }

  public Map<String, GraphOptions> getOnTransitions() {
    return onTransitions;
  }

  /**
   * Sets the {@code Map} of possible onTransitions for the given graph node. If the Node is {@code
   * Composite} only {@code _success} and {@code _error} transitions can be configured.
   *
   * @param onTransitions map of possible transitions.
   * @return reference to this, so the API can be used fluently
   */
  public GraphOptions setOnTransitions(
      Map<String, GraphOptions> onTransitions) {
    this.onTransitions = onTransitions;
    return this;
  }

  /**
   * Sets {@code Action} name. This action will be executed during processing given graph node. If
   * {@code action} field is defined, Node configured by it will be treated as Action Node.
   *
   * @param actionName action name
   * @return reference to this, so the API can be used fluently
   */
  public GraphOptions setAction(String actionName) {
    node.setFactory(ACTION);
    node.setConfig(new ActionNodeConfigOptions(actionName).toJson());
    return this;
  }

  @Deprecated
  public GraphOptions setActions(List<GraphOptions> subTasks) {
    setSubTasks(subTasks);
    return this;
  }

  public GraphOptions setSubtasks(List<GraphOptions> subtasks) {
    setSubTasks(subtasks);
    return this;
  }

  public GraphOptions setSubTasks(List<GraphOptions> subTasks) {
    node.setFactory(SUBTASKS);
    node.setConfig(new SubTasksNodeConfigOptions(subTasks).toJson());
    return this;
  }

  // TODO remove when node factories finished
  public boolean isComposite() {
    return SUBTASKS.equals(node.getFactory());
  }

}
