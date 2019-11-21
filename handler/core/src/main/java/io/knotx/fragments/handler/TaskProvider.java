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
package io.knotx.fragments.handler;

import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.exception.TaskFactoryNotFoundException;
import io.knotx.fragments.task.TaskFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

class TaskProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskProvider.class);

  private List<TaskFactory> factories;
  private final Vertx vertx;

  TaskProvider(List<TaskFactoryOptions> factoryOptions, Vertx vertx) {
    this.vertx = vertx;
    factories = initFactories(factoryOptions);
  }

  Optional<Task> newInstance(FragmentEventContext eventContext) {
    return factories.stream()
        .filter(f -> f.accept(eventContext))
        .findFirst()
        .map(f -> f.newInstance(eventContext));
  }

  private List<TaskFactory> initFactories(List<TaskFactoryOptions> optionsList) {
    Map<String, TaskFactory> loadedFactories = loadFactories();

    List<TaskFactory> result = new ArrayList<>();
    optionsList.iterator().forEachRemaining(options -> result.add(
        configureFactory(loadedFactories, options.getFactory(), options.getConfig())));
    return result;
  }

  private TaskFactory configureFactory(Map<String, TaskFactory> loadedFactories, String factory,
      JsonObject config) {
    LOGGER.debug("Initializing task factory [{}] with config [{}]", factory, config);
    return Optional.ofNullable(loadedFactories.get(factory))
        .map(f -> f.configure(config, vertx))
        .orElseThrow(() -> new TaskFactoryNotFoundException(factory));
  }

  private Map<String, TaskFactory> loadFactories() {
    Map<String, TaskFactory> factories = new HashMap<>();
    ServiceLoader
        .load(TaskFactory.class).iterator()
        .forEachRemaining(f -> {
          LOGGER.info("Registering task factory [{}]", f.getName());
          factories.put(f.getName(), f);
        });

    return factories;
  }
}
