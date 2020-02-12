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
import io.knotx.fragments.handler.api.exception.ConfigurationException;
import io.knotx.fragments.task.TaskWithMetadata;
import io.knotx.fragments.task.NodeWithMetadata;
import io.knotx.fragments.task.TaskFactory;
import io.knotx.fragments.task.exception.NodeFactoryNotFoundException;
import io.knotx.fragments.task.factory.node.NodeFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefaultTaskFactory implements TaskFactory, NodeProvider {

  public static final String NAME = "default";

  private DefaultTaskFactoryConfig taskFactoryConfig;
  private Map<String, NodeFactory> nodeFactories;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public DefaultTaskFactory configure(JsonObject taskFactoryConfig, Vertx vertx) {
    this.taskFactoryConfig = new DefaultTaskFactoryConfig(taskFactoryConfig);
    nodeFactories = initFactories(vertx);
    return this;
  }

  @Override
  public boolean accept(FragmentEventContext eventContext) {
    Fragment fragment = eventContext.getFragmentEvent().getFragment();
    boolean fragmentContainsTask = fragment.getConfiguration()
        .containsKey(taskFactoryConfig.getTaskNameKey());
    return fragmentContainsTask && isTaskConfigured(fragment);
  }

  private boolean isTaskConfigured(Fragment fragment) {
    String taskName = fragment.getConfiguration().getString(taskFactoryConfig.getTaskNameKey());
    return taskFactoryConfig.getTasks().containsKey(taskName);
  }

  @Override
  public TaskWithMetadata newInstance(FragmentEventContext eventContext) {
    Fragment fragment = eventContext.getFragmentEvent().getFragment();
    String taskKey = taskFactoryConfig.getTaskNameKey();
    String taskName = fragment.getConfiguration().getString(taskKey);

    Map<String, GraphNodeOptions> tasks = taskFactoryConfig.getTasks();
    return Optional.ofNullable(tasks.get(taskName))
        .map(rootGraphNodeOptions -> {
          NodeWithMetadata rootNode = initNode(rootGraphNodeOptions);
          return new TaskWithMetadata(taskName, rootNode);
        })
        .orElseThrow(() -> new ConfigurationException("Task [" + taskName + "] not configured!"));
  }

  @Override
  public NodeWithMetadata initNode(GraphNodeOptions nodeOptions) {
    return findNodeFactory(nodeOptions)
        .map(f -> f.initNode(nodeOptions, initTransitions(nodeOptions), this))
        .orElseThrow(() -> new NodeFactoryNotFoundException(nodeOptions.getNode().getFactory()));
  }

  private Optional<NodeFactory> findNodeFactory(GraphNodeOptions nodeOptions) {
    return Optional.ofNullable(nodeFactories.get(nodeOptions.getNode().getFactory()));
  }

  private Map<String, NodeWithMetadata> initTransitions(GraphNodeOptions nodeOptions) {
    Map<String, GraphNodeOptions> transitions = nodeOptions.getOnTransitions();
    Map<String, NodeWithMetadata> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> edges
        .put(transition, initNode(childGraphOptions)));
    return edges;
  }

  private Map<String, NodeFactory> initFactories(Vertx vertx) {
    ServiceLoader<NodeFactory> factories = ServiceLoader.load(NodeFactory.class);
    return taskFactoryConfig.getNodeFactories().stream()
        .map(options -> {
          NodeFactory factory = findNodeFactory(factories, options.getFactory());
          return factory.configure(options.getConfig(), vertx);
        }).collect(Collectors.toMap(NodeFactory::getName, f -> f));
  }

  private NodeFactory findNodeFactory(ServiceLoader<NodeFactory> factories, String factory) {
    Stream<NodeFactory> factoryStream = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(factories.iterator(), Spliterator.ORDERED),
        false);

    return factoryStream
        .filter(f -> f.getName().equals(factory))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Node not defined"));
  }
}
