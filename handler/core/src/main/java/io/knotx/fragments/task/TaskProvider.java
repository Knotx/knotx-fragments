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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class TaskProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskProvider.class);

  private final String taskKey;
  private final Map<String, TaskFactory> factories;
  private final Map<String, TaskOptions> tasks;
  private final Vertx vertx;

  public TaskProvider(String taskKey, Map<String, TaskOptions> tasks, Vertx vertx) {
    this.taskKey = taskKey;
    this.tasks = tasks;
    factories = initFactories();
    this.vertx = vertx;
  }

  public Optional<Task> newInstance(FragmentEventContext fragmentEventContext) {
    return Optional.of(fragmentEventContext.getFragmentEvent().getFragment())
        .filter(this::hasTask)
        .map(this::getTaskName)
        .map(taskName -> {
          TaskOptions taskOptions = tasks.get(taskName);
          if (taskOptions == null) {
            LOGGER.error("Could not find task [{}] in tasks [{}]", taskName, tasks);
            throw new TaskNotFoundException(taskName);
          }
          taskOptions.getConfig();
          TaskDefinition taskDefinition = getTaskConfiguration(taskName);
          return newInstance(taskDefinition, taskOptions.getFactory(), taskOptions.getConfig());
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

  private Task newInstance(TaskDefinition taskDefinition, String factoryName,
      JsonObject factoryOptions) {
    return Optional.ofNullable(factories.get(factoryName))
        .map(f -> f.newInstance(taskDefinition.getTaskName(), taskDefinition.getGraphNodeOptions(),
            factoryOptions, vertx))
        .orElseThrow(() -> new GraphConfigurationException("Could not find task builder"));
  }

  private Map<String, TaskFactory> initFactories() {
    Map<String, TaskFactory> factories = new HashMap<>();
    ServiceLoader
        .load(TaskFactory.class).iterator()
        .forEachRemaining(f -> factories.put(f.getName(), f));
    return factories;
  }

}
