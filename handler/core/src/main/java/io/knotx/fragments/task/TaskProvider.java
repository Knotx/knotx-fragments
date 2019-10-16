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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.exception.GraphConfigurationException;
import io.knotx.fragments.handler.exception.TaskNotFoundException;
import io.knotx.fragments.handler.options.TaskOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class TaskProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskProvider.class);

  private String taskKey;
  private List<TaskBuilderFactory> builderFactories;
  private Map<String, TaskOptions> tasks;
  private ActionProvider actionProvider;

  public TaskProvider(String taskKey, Map<String, TaskOptions> tasks,
      ActionProvider actionProvider) {
    this.taskKey = taskKey;
    this.tasks = tasks;
    this.actionProvider = actionProvider;
    builderFactories = initBuilders(actionProvider);
  }

  public Optional<Task> get(FragmentEventContext fragmentEventContext) {
    Fragment fragment = fragmentEventContext.getFragmentEvent().getFragment();
    if (hasTask(fragment)) {
      String taskName = getTaskName(fragment);

      TaskBuilder builder = getBuilder(taskName);
      Configuration taskConfig = getTaskConfiguration(taskName);

      return Optional
          .of(builder.get(taskConfig, fragmentEventContext));
    } else {
      return Optional.empty();
    }
  }

  private String getTaskName(Fragment fragment) {
    return fragment.getConfiguration().getString(taskKey);
  }

  private boolean hasTask(Fragment fragment) {
    return fragment.getConfiguration().containsKey(taskKey);
  }

  private Configuration getTaskConfiguration(String taskName) {
    return new Configuration(taskName, tasks.get(taskName).getConfig());
  }

  private TaskBuilder getBuilder(String taskName) {
    TaskOptions taskOptions = tasks.get(taskName);
    if (taskOptions == null) {
      LOGGER.error("Could not find task [{}] in tasks [{}]", taskName, tasks);
      throw new TaskNotFoundException(taskName);
    }
    String builderName = taskOptions.getBuilder().getName();
    return builderFactories.stream()
        .filter(f -> f.getName().equals(builderName))
        .findFirst()
        .map(f -> f.create(actionProvider))
        .orElseThrow(() -> new GraphConfigurationException("Could not find task builder"));
  }

  private List<TaskBuilderFactory> initBuilders(ActionProvider actionProvider) {
    List<TaskBuilderFactory> builders = new ArrayList<>();
    ServiceLoader
        .load(TaskBuilderFactory.class).iterator()
        .forEachRemaining(builders::add);
    return builders;
  }
}
