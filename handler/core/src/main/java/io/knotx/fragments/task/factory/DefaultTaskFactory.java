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
import static io.knotx.fragments.task.factory.TaskOptions.NODE_LOG_LEVEL_KEY;

import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.TaskDefinition;
import io.knotx.fragments.task.TaskFactory;
import io.knotx.fragments.task.exception.GraphConfigurationException;
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
  public Task newInstance(TaskDefinition taskDefinition, JsonObject taskOptions, Vertx vertx) {
    TaskOptions options = initTaskOptions(taskOptions);
    Node rootNode = initNode(taskDefinition.getGraphNodeOptions(), options, vertx);
    return new Task(taskDefinition.getTaskName(), rootNode);
  }

  private TaskOptions initTaskOptions(JsonObject taskOptions) {
    TaskOptions options = new TaskOptions(taskOptions);
    Map<String, ActionOptions> actionNameToOptions = options.getActions();
    if (actionNameToOptions == null) {
      throw new GraphConfigurationException("The 'actions' property not configured!");
    }
    initOptions(actionNameToOptions, options.getLogLevel());
    return options;
  }

  private Node initNode(GraphNodeOptions nodeOptions,
      TaskOptions options, Vertx vertx) {
    Map<String, Node> transitionToNodeMap = initTransitions(nodeOptions, options, vertx);
    final Node node;
    if (GraphNodeOptions.SUBTASKS.equals(nodeOptions.getNode().getFactory())) {
      node = new SubtasksNodeFactory()
          .newInstance(nodeOptions, transitionToNodeMap, options, vertx);
    } else {
      node = new ActionNodeFactory().newInstance(nodeOptions, transitionToNodeMap, options, vertx);
    }
    return node;
  }

  private Map<String, Node> initTransitions(GraphNodeOptions nodeOptions, TaskOptions options,
      Vertx vertx) {
    Map<String, GraphNodeOptions> transitions = nodeOptions.getOnTransitions();
    Map<String, Node> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> edges
        .put(transition, initNode(childGraphOptions, options, vertx)));
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

  private static class ActionNodeFactory implements NodeFactory {

    private ActionProvider actionProvider;

    public ActionNodeFactory() {
      actionProvider = new ActionProvider(supplyFactories());
    }

    @Override
    public String getName() {
      return "action";
    }

    @Override
    public Node newInstance(GraphNodeOptions options, Map<String, Node> edges,
        TaskOptions task, Vertx vertx) {
      ActionNodeConfigOptions config = new ActionNodeConfigOptions(options.getNode().getConfig());
      Action action = actionProvider.get(config.getAction(), task.getActions(), vertx).orElseThrow(
          () -> new GraphConfigurationException("No provider for action " + config.getAction()));
      return new SingleNode(config.getAction(), toRxFunction(action), edges);
    }

    private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
        Action action) {
      io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
          .newInstance(action);
      return rxAction::rxApply;
    }

    private Supplier<Iterator<ActionFactory>> supplyFactories() {
      return () -> {
        ServiceLoader<ActionFactory> factories = ServiceLoader
            .load(ActionFactory.class);
        return factories.iterator();
      };
    }
  }

  private class SubtasksNodeFactory implements NodeFactory {

    @Override
    public String getName() {
      return "subtasks";
    }

    public Node newInstance(GraphNodeOptions nodeOptions, Map<String, Node> edges,
        TaskOptions taskOptions, Vertx vertx) {
      SubtasksNodeConfigOptions config = new SubtasksNodeConfigOptions(
          nodeOptions.getNode().getConfig());
      List<Node> nodes = config.getSubtasks().stream()
          .map((GraphNodeOptions nextNodeOptions) -> initNode(nextNodeOptions, taskOptions, vertx))
          .collect(Collectors.toList());
      return new CompositeNode(getNodeId(), nodes, edges.get(SUCCESS_TRANSITION),
          edges.get(ERROR_TRANSITION));
    }

    private String getNodeId() {
      // TODO this value should be calculated based on graph, the behaviour now is not changed
      return "composite";
    }
  }
}
