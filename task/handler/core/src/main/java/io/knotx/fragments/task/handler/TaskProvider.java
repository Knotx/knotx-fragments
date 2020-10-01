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
package io.knotx.fragments.task.handler;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.factory.api.TaskFactory;
import io.knotx.fragments.task.factory.api.metadata.TaskWithMetadata;
import io.knotx.fragments.task.handler.exception.TaskFactoryNotFoundException;
import io.knotx.fragments.task.handler.spi.FactoryOptions;
import io.knotx.server.api.context.ClientRequest;
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

  TaskProvider(List<FactoryOptions> factoryOptions, Vertx vertx) {
    this.vertx = vertx;
    factories = initFactories(factoryOptions);
  }

  Optional<TaskWithMetadata> newInstance(Fragment fragment, ClientRequest clientRequest) {
    return factories.stream()
        .filter(f -> f.accept(fragment, clientRequest))
        .findFirst()
        .map(f -> {
          LOGGER.debug("Task factory [{}] accepts fragment [{}]", f.getName(),
              fragment.getId());
          return f;
        })
        .map(f -> f.newInstance(fragment, clientRequest));
  }

  private List<TaskFactory> initFactories(List<FactoryOptions> optionsList) {
    Map<String, TaskFactory> loadedFactories = loadFactories();

    List<TaskFactory> result = new ArrayList<>();
    optionsList.forEach(options -> result.add(
        configureFactory(loadedFactories, options.getFactory(), options.getConfig())));
    return result;
  }

  private TaskFactory configureFactory(Map<String, TaskFactory> loadedFactories, String factory,
      JsonObject config) {
    LOGGER.info("Initializing task factory [{}] with config [{}]", factory, config);
    return Optional.ofNullable(loadedFactories.get(factory))
        .map(f -> f.configure(config, vertx))
        .orElseThrow(() -> new TaskFactoryNotFoundException(factory));
  }

  private Map<String, TaskFactory> loadFactories() {
    Map<String, TaskFactory> loadedFactories = new HashMap<>();
    ServiceLoader
        .load(TaskFactory.class).iterator()
        .forEachRemaining(f -> {
          LOGGER.debug("Registering task factory [{}]", f.getName());
          loadedFactories.put(f.getName(), f);
        });

    return loadedFactories;
  }
}
