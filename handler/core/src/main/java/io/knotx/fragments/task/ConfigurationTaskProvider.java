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

import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.exception.GraphConfigurationException;
import io.knotx.fragments.task.options.ActionNodeConfigOptions;
import io.knotx.fragments.task.options.SubTasksNodeConfigOptions;
import io.knotx.fragments.task.options.GraphOptions;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigurationTaskProvider implements TaskProvider {

  private final ActionProvider actionProvider;

  ConfigurationTaskProvider(ActionProvider proxyProvider) {
    this.actionProvider = proxyProvider;
  }

  @Override
  public Task get(Configuration config, FragmentEventContext event) {
    Node rootNode = initGraphNode(config.getRootNode());
    return new Task(config.getTaskName(), rootNode);
  }

  private Node initGraphNode(GraphOptions options) {
    Map<String, GraphOptions> transitions = options.getOnTransitions();
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

  private Node buildActionNode(GraphOptions options, Map<String, Node> edges) {
    ActionNodeConfigOptions config = new ActionNodeConfigOptions(options.getNode().getConfig());
    Action action = actionProvider.get(config.getAction()).orElseThrow(
        () -> new GraphConfigurationException("No provider for action " + config.getAction()));
    return new ActionNode(config.getAction(), toRxFunction(action), edges);
  }

  private Node buildCompositeNode(GraphOptions options, Map<String, Node> edges) {
    SubTasksNodeConfigOptions config = new SubTasksNodeConfigOptions(
        options.getNode().getConfig());
    List<Node> nodes = config.getSubTasks().stream()
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
