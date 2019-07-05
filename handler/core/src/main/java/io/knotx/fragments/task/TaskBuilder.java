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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.task;

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.exception.GraphConfigurationException;
import io.knotx.fragments.handler.options.FragmentsHandlerOptions;
import io.knotx.fragments.handler.options.NodeOptions;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TaskBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskBuilder.class);

  private final String taskKey;
  private final Map<String, NodeOptions> tasks;
  private final ActionProvider actionProvider;

  public TaskBuilder(String taskKey, Map<String, NodeOptions> tasks, ActionProvider proxyProvider) {
    this.taskKey = taskKey;
    this.tasks = tasks;
    this.actionProvider = proxyProvider;
  }

  TaskBuilder(Map<String, NodeOptions> tasks, ActionProvider proxyProvider) {
    this.taskKey = FragmentsHandlerOptions.DEFAULT_TASK_KEY;
    this.tasks = tasks;
    this.actionProvider = proxyProvider;
  }

  public Optional<Task> build(Fragment fragment) {
    Optional<Task> result = Optional.empty();
    if (fragment.getConfiguration().containsKey(taskKey)) {
      String task = fragment.getConfiguration().getString(taskKey);
      NodeOptions options = tasks.get(task);
      if (options == null) {
        LOGGER.warn("Task [{}] not defined in configuration!", task);
        return Optional.empty();
      }

      Node rootNode = initGraphNode(options);
      result = Optional.of(new Task(task, rootNode));
    }
    return result;
  }

  private Node initGraphNode(NodeOptions options) {
    Map<String, NodeOptions> transitions = options.getOnTransitions();
    Map<String, Node> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> {
      edges.put(transition, initGraphNode(childGraphOptions));
    });
    final Node node;
    if (options.isComposite()) {
      node = buildCompositeNode(options, edges);
    } else {
      node = buildActionNode(options, edges);
    }
    return node;
  }

  private Node buildActionNode(NodeOptions options, Map<String, Node> edges) {
    Action action = actionProvider.get(options.getAction()).orElseThrow(
        () -> new GraphConfigurationException("No provider for action " + options.getAction()));
    return new ActionNode(options.getAction(), toRxFunction(action), edges);
  }

  private Node buildCompositeNode(NodeOptions options, Map<String, Node> edges) {
    List<Node> nodes = options.getActions().stream()
        .map(this::initGraphNode)
        .collect(Collectors.toList());
    return new CompositeNode(nodes, edges.get(SUCCESS_TRANSITION), edges.get(ERROR_TRANSITION));
  }

  private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
      Action action) {
    io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
        .newInstance(action);
    return rxAction::rxApply;
  }

}
