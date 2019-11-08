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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.task.exception.GraphConfigurationException;
import io.knotx.fragments.task.exception.TaskNotFoundException;
import io.knotx.fragments.task.options.TaskOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class TaskFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskFactory.class);

  private final String taskKey;
  private final Map<String, TaskProviderFactory> providersFactories;
  private final Map<String, TaskOptions> tasks;
  private final Vertx vertx;

  public TaskFactory(String taskKey, Map<String, TaskOptions> tasks, Vertx vertx) {
    this.taskKey = taskKey;
    this.tasks = tasks;
    providersFactories = initProviders();
    this.vertx = vertx;
  }

  public Optional<Task> newInstance(FragmentEventContext fragmentEventContext) {
    return Optional.of(fragmentEventContext.getFragmentEvent().getFragment())
        .filter(this::hasTask)
        .map(this::getTaskName)
        .map(taskName -> {
          TaskProvider factory = getProvider(taskName);
          TaskDefinition taskConfig = getTaskConfiguration(taskName);
          return factory.newInstance(taskConfig, fragmentEventContext);
        });
  }

  private String getTaskName(Fragment fragment) {
    return fragment.getConfiguration().getString(taskKey);
  }

  private boolean hasTask(Fragment fragment) {
    return fragment.getConfiguration().containsKey(taskKey);
  }

  private TaskDefinition getTaskConfiguration(String taskName) {
    return new TaskDefinition(taskName, tasks.get(taskName).getGraph());
  }

  private TaskProvider getProvider(String taskName) {
    TaskOptions taskOptions = tasks.get(taskName);
    if (taskOptions == null) {
      LOGGER.error("Could not find task [{}] in tasks [{}]", taskName, tasks);
      throw new TaskNotFoundException(taskName);
    }
    String factoryName = taskOptions.getFactory();
    return Optional.ofNullable(providersFactories.get(factoryName))
        .map(f -> f.create(taskOptions.getConfig(), vertx))
        .orElseThrow(() -> new GraphConfigurationException("Could not find task builder"));
  }

  private Map<String, TaskProviderFactory> initProviders() {
    Map<String, TaskProviderFactory> factories = new HashMap<>();
    ServiceLoader
        .load(TaskProviderFactory.class).iterator()
        .forEachRemaining(f -> factories.put(f.getName(), f));
    return factories;
  }

}
