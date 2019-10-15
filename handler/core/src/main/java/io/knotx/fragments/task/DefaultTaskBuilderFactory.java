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
import io.knotx.fragments.handler.exception.GraphConfigurationException;
import io.knotx.fragments.handler.options.NodeOptions;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultTaskBuilderFactory implements TaskBuilderFactory {

  @Override
  public String getName() {
    return "default";
  }

  @Override
  public TaskBuilder create(ActionProvider proxyProvider) {
    return new DefaultTaskProvider(proxyProvider);
  }

  static class DefaultTaskProvider implements TaskBuilder {

    private final ActionProvider actionProvider;

    DefaultTaskProvider(ActionProvider proxyProvider) {
      this.actionProvider = proxyProvider;
    }

    public Task get(Configuration config, FragmentEventContext eventContext) {
      Node rootNode = initGraphNode(config.getRootNode());
      return new Task(config.getTaskName(), rootNode);
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

}
