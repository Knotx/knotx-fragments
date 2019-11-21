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
package io.knotx.fragments.task.factory;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.task.TaskFactory;
import io.knotx.fragments.task.exception.NodeFactoryNotFoundException;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.fragments.task.options.TaskOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class DefaultTaskFactory implements TaskFactory, NodeProvider {

  public static final String NAME = "default";
  private static final String TASK_NAME_KEY_OPTION = "taskNameKey";
  public static final String DEFAULT_TASK_NAME_KEY = "data-knotx-task";

  private JsonObject factoryConfig;
  private Map<String, NodeFactory> nodeFactories;

  public DefaultTaskFactory() {
    nodeFactories = initNodeFactories();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public DefaultTaskFactory configure(JsonObject factoryConfig) {
    this.factoryConfig = factoryConfig;
    return this;
  }

  @Override
  public boolean accept(FragmentEventContext eventContext) {
    String taskNameKey = factoryConfig.getString(TASK_NAME_KEY_OPTION, DEFAULT_TASK_NAME_KEY);
    return eventContext.getFragmentEvent().getFragment().getConfiguration()
        .containsKey(taskNameKey);
  }

  @Override
  public Task newInstance(FragmentEventContext eventContext, Vertx vertx) {
    Fragment fragment = eventContext.getFragmentEvent().getFragment();
    String taskKey = factoryConfig.getString(TASK_NAME_KEY_OPTION, DEFAULT_TASK_NAME_KEY);

    String taskName = fragment.getConfiguration().getString(taskKey);

    JsonObject taskOptionsJson = factoryConfig.getJsonObject("tasks").getJsonObject(taskName);

    GraphNodeOptions nodeOptions = new TaskOptions(taskOptionsJson).getGraph();
    Node rootNode = initNode(taskName, nodeOptions, factoryConfig, vertx);
    return new Task(taskName, rootNode);
  }

  @Override
  public Node initNode(String taskName, GraphNodeOptions nodeOptions, JsonObject taskConfig,
      Vertx vertx) {
    Map<String, Node> transitionToNodeMap = initTransitions(taskName, nodeOptions, taskConfig,
        vertx);
    Optional<NodeFactory> nodeFactory = getNodeFactory(nodeOptions);
    return nodeFactory
        .map(f -> f.initNode(nodeOptions, transitionToNodeMap, taskName, taskConfig, this, vertx))
        .orElseThrow(() -> new NodeFactoryNotFoundException(nodeOptions.getNode().getFactory()));
  }

  private Optional<NodeFactory> getNodeFactory(GraphNodeOptions nodeOptions) {
    return Optional.ofNullable(nodeFactories.get(nodeOptions.getNode().getFactory()));
  }

  private Map<String, Node> initTransitions(String taskName, GraphNodeOptions nodeOptions,
      JsonObject taskConfig,
      Vertx vertx) {
    Map<String, GraphNodeOptions> transitions = nodeOptions.getOnTransitions();
    Map<String, Node> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> edges
        .put(transition, initNode(taskName, childGraphOptions, taskConfig, vertx)));
    return edges;
  }

  private Map<String, NodeFactory> initNodeFactories() {
    ServiceLoader<NodeFactory> factories = ServiceLoader.load(NodeFactory.class);
    Map<String, NodeFactory> nodeFactories = new HashMap<>();
    factories.iterator()
        .forEachRemaining(nodeFactory -> nodeFactories.put(nodeFactory.getName(), nodeFactory));
    return nodeFactories;
  }
}