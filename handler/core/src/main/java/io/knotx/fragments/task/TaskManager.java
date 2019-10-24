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
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.task.exception.GraphConfigurationException;
import io.knotx.fragments.task.exception.TaskNotFoundException;
import io.knotx.fragments.task.options.TaskOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class TaskManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);

  private String taskKey;
  private List<TaskProviderFactory> providersFactories;
  private Map<String, TaskOptions> tasks;
  private ActionProvider actionProvider;

  public TaskManager(String taskKey, Map<String, TaskOptions> tasks,
      ActionProvider actionProvider) {
    this.taskKey = taskKey;
    this.tasks = tasks;
    this.actionProvider = actionProvider;
    providersFactories = initProviders();
  }

  public Optional<Task> newInstance(FragmentEventContext fragmentEventContext) {
    return Optional.of(fragmentEventContext.getFragmentEvent().getFragment())
        .filter(this::hasTask)
        .map(this::getTaskName)
        .map(taskName -> {
          TaskProvider builder = getProvider(taskName);
          Configuration taskConfig = getTaskConfiguration(taskName);
          return builder.get(taskConfig, fragmentEventContext);
        });
  }

  private String getTaskName(Fragment fragment) {
    return fragment.getConfiguration().getString(taskKey);
  }

  private boolean hasTask(Fragment fragment) {
    return fragment.getConfiguration().containsKey(taskKey);
  }

  private Configuration getTaskConfiguration(String taskName) {
    return new Configuration(taskName, tasks.get(taskName).getGraph());
  }

  private TaskProvider getProvider(String taskName) {
    TaskOptions taskOptions = tasks.get(taskName);
    if (taskOptions == null) {
      LOGGER.error("Could not find task [{}] in tasks [{}]", taskName, tasks);
      throw new TaskNotFoundException(taskName);
    }
    String builderName = taskOptions.getFactory();
    return providersFactories.stream()
        .filter(f -> f.getName().equals(builderName))
        .findFirst()
        .map(f -> f.create(taskOptions.getConfig(), actionProvider))
        .orElseThrow(() -> new GraphConfigurationException("Could not find task builder"));
  }

  private List<TaskProviderFactory> initProviders() {
    List<TaskProviderFactory> builders = new ArrayList<>();
    ServiceLoader
        .load(TaskProviderFactory.class).iterator()
        .forEachRemaining(builders::add);
    return builders;
  }

}
