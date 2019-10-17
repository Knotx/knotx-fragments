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
package io.knotx.fragments.task;

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.Fragment;
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
import io.knotx.fragments.task.models.TemplateTaskActionData;
import io.knotx.fragments.task.models.TemplateTaskData;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class TemplateTaskBuilderFactory implements TaskBuilderFactory {

  @Override
  public String getName() {
    return "template";
  }

  @Override
  public TaskBuilder create(ActionProvider actionProvider) {
    return new TemplateTaskBuilder(actionProvider);
  }

  private static class TemplateTaskBuilder implements TaskBuilder {

    private final ActionProvider actionProvider;

    public TemplateTaskBuilder(ActionProvider actionProvider) {
      this.actionProvider = actionProvider;
    }

    public Task get(Configuration config, FragmentEventContext eventContext) {
      Node rootNode = initGraphNode(config.getRootNode(), eventContext);
      return new Task(config.getTaskName(), rootNode);
    }

    public Node initGraphNode(NodeOptions options, FragmentEventContext eventContext) {
      Map<String, NodeOptions> transitions = options.getOnTransitions();
      Map<String, Node> edges = new HashMap<>();
      transitions.forEach((transition, childGraphOptions) -> {
        edges.put(transition, initGraphNode(childGraphOptions, eventContext));
      });
      final Node node;
      if (options.isComposite()) {
        if (isTemplate(options)) {
          node = buildTemplateNode(options, edges, eventContext);
        } else {
          node = buildCompositeNode(options, edges, eventContext);
        }
      } else {
        node = buildActionNode(options, edges);
      }
      return node;
    }

    private boolean isTemplate(NodeOptions options) {
      return StringUtils.isNotBlank(options.getTemplate());
    }

    private Node buildTemplateNode(NodeOptions options, Map<String, Node> edges,
        FragmentEventContext eventContext) {
      Fragment fragment = eventContext.getFragmentEvent().getFragment();
      String templateActions = fragment.getConfiguration().getString("data-knotx-task-actions");
      TemplateTaskData templateTaskData = new TemplateTaskData(new JsonObject(templateActions));

      List<TemplateTaskActionData> actions = templateTaskData.get(options.getTemplate());
      List<Node> nodes = actions.stream()
          .map(a -> {
            Action action = actionProvider.get(a.getAction(), a.getNamespace(), a.getExtra()).orElseThrow(
                () -> new GraphConfigurationException("No provider for action " + options.getAction()));
            return new ActionNode(a.getAction(), toRxFunction(action));
          }).collect(Collectors.toList());
      return new CompositeNode(nodes, edges.get(SUCCESS_TRANSITION), edges.get(ERROR_TRANSITION));
    }

    public Node buildActionNode(NodeOptions options, Map<String, Node> edges) {
      Action action = actionProvider.get(options.getAction()).orElseThrow(
          () -> new GraphConfigurationException("No provider for action " + options.getAction()));
      return new ActionNode(options.getAction(), toRxFunction(action), edges);
    }

    public Node buildCompositeNode(NodeOptions options, Map<String, Node> edges,
        FragmentEventContext eventContext) {
      List<Node> nodes = options.getActions().stream()
          .map((NodeOptions o) -> initGraphNode(o, eventContext))
          .collect(Collectors.toList());
      return new CompositeNode(nodes, edges.get(SUCCESS_TRANSITION), edges.get(ERROR_TRANSITION));
    }

    public Function<FragmentContext, Single<FragmentResult>> toRxFunction(
        Action action) {
      io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
          .newInstance(action);
      return rxAction::rxApply;
    }
  }
}
