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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.task.factory.TaskFactoryOptions.NODE_LOG_LEVEL_KEY;

import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.TaskFactory;
import io.knotx.fragments.task.exception.GraphConfigurationException;
import io.knotx.fragments.task.exception.NodeFactoryNotFoundException;
import io.knotx.fragments.task.options.ActionNodeConfigOptions;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.fragments.task.options.SubtasksNodeConfigOptions;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultTaskFactory implements TaskFactory {

  public static final String NAME = "default";

  private Map<String, NodeFactory> nodeFactories;

  public DefaultTaskFactory() {
    nodeFactories = initNodeFactories();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Task newInstance(String taskName, GraphNodeOptions nodeOptions, JsonObject taskOptions,
      Vertx vertx) {
    TaskFactoryOptions options = initTaskOptions(taskOptions);
    Node rootNode = initNode(taskName, nodeOptions, options, vertx);
    return new Task(taskName, rootNode);
  }

  private TaskFactoryOptions initTaskOptions(JsonObject taskOptions) {
    TaskFactoryOptions options = new TaskFactoryOptions(taskOptions);
    Map<String, ActionOptions> actionNameToOptions = options.getActions();
    if (actionNameToOptions == null) {
      throw new GraphConfigurationException("The 'actions' property not configured!");
    }
    initOptions(actionNameToOptions, options.getLogLevel());
    return options;
  }

  public Node initNode(String taskName, GraphNodeOptions nodeOptions,
      TaskFactoryOptions options, Vertx vertx) {
    Map<String, Node> transitionToNodeMap = initTransitions(taskName, nodeOptions, options, vertx);
    Optional<NodeFactory> nodeFactory = getNodeFactory(nodeOptions);
    return nodeFactory
        .map(f -> f.newInstance(taskName, nodeOptions, transitionToNodeMap, options, this, vertx))
        .orElseThrow(() -> new NodeFactoryNotFoundException(nodeOptions.getNode().getFactory()));
  }

  private Optional<NodeFactory> getNodeFactory(GraphNodeOptions nodeOptions) {
    return Optional.ofNullable(nodeFactories.get(nodeOptions.getNode().getFactory()));
  }

  private Map<String, Node> initTransitions(String taskName, GraphNodeOptions nodeOptions,
      TaskFactoryOptions options,
      Vertx vertx) {
    Map<String, GraphNodeOptions> transitions = nodeOptions.getOnTransitions();
    Map<String, Node> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> edges
        .put(transition, initNode(taskName, childGraphOptions, options, vertx)));
    return edges;
  }

  private void initOptions(Map<String, ActionOptions> nodeNameToOptions, String logLevel) {
    nodeNameToOptions.values().stream()
        .map(options -> {
          JsonObject config = options.getConfig();
          if (config.fieldNames().contains(NODE_LOG_LEVEL_KEY)) {
            return config;
          }
          return config.put(NODE_LOG_LEVEL_KEY, logLevel);
        });
  }

  private Map<String, NodeFactory> initNodeFactories() {
    ServiceLoader<NodeFactory> factories = ServiceLoader.load(NodeFactory.class);
    Map<String, NodeFactory> nodeFactories = new HashMap<>();
    factories.iterator().forEachRemaining(nodeFactory -> {
      nodeFactories.put(nodeFactory.getName(), nodeFactory);
    });
    return nodeFactories;
  }

}
