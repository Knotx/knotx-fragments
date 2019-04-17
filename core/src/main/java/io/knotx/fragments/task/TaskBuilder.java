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

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.fragment.Action;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.knotx.fragments.handler.exception.GraphConfigurationException;
import io.knotx.fragments.handler.options.NodeOptions;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class TaskBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskBuilder.class);
  static final String TASK_KEY = "data-knotx-task";

  private final Map<String, NodeOptions> tasks;
  private final ActionProvider actionProvider;

  public TaskBuilder(Map<String, NodeOptions> tasks, ActionProvider proxyProvider) {
    this.tasks = tasks;
    this.actionProvider = proxyProvider;
  }

  public Optional<Task> build(Fragment fragment) {
    Optional<Task> result = Optional.empty();
    if (fragment.getConfiguration().containsKey(TASK_KEY)) {
      String task = fragment.getConfiguration().getString(TASK_KEY);
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
    Action action = actionProvider.get(options.getAction()).orElseThrow(
        () -> new GraphConfigurationException("No provider for action " + options.getAction()));
    Map<String, NodeOptions> transitions = options.getOnTransitions();
    Map<String, Node> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> {
      edges.put(transition, initGraphNode(childGraphOptions));
    });
    //ToDo - create single operation or parallel here
    return new ActionNode(options.getAction(), toRxFunction(action), edges);
  }

  private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
      Action action) {
    io.knotx.fragments.handler.reactivex.api.fragment.Action rxAction = io.knotx.fragments.handler.reactivex.api.fragment.Action
        .newInstance(action);
    return rxAction::rxApply;
  }

}
