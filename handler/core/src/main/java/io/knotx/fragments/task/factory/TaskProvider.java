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
package io.knotx.fragments.task.factory;

import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.task.TaskFactory;
import io.knotx.fragments.task.exception.TaskFactoryNameNotDefinedException;
import io.vertx.core.json.JsonArray;
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

public class TaskProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskProvider.class);

  private final Vertx vertx;

  private List<TaskFactory> orderedFactories;

  public TaskProvider(JsonArray factoryOptions, Vertx vertx) {
    orderedFactories = init(factoryOptions);
    this.vertx = vertx;
  }

  public Optional<Task> newInstance(FragmentEventContext eventContext) {
    return orderedFactories.stream()
        .filter(f -> f.accept(eventContext))
        .findFirst()
        .map(f -> f.newInstance(eventContext, vertx));
  }

  private List<TaskFactory> init(JsonArray factoryOptionsArray) {
    Map<String, TaskFactory> loadedFactories = loadFactories();

    List<TaskFactory> orderedFactories = new ArrayList<>();
    factoryOptionsArray.iterator().forEachRemaining(options -> {
      JsonObject factoryOptions = (JsonObject) options;
      orderedFactories.add(
          configureFactory(loadedFactories, getName(factoryOptions), getConfig(factoryOptions)));
    });
    return orderedFactories;
  }

  private TaskFactory configureFactory(Map<String, TaskFactory> loadedFactories, String factoryName,
      JsonObject config) {
    TaskFactory factory = loadedFactories.get(factoryName);
    factory.configure(config);
    return factory;
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

  private JsonObject getConfig(JsonObject factoryOptions) {
    return factoryOptions.getJsonObject("config", new JsonObject());
  }

  private String getName(JsonObject factoryOptions) {
    return Optional.ofNullable(factoryOptions.getString("name"))
        .orElseThrow(() -> new TaskFactoryNameNotDefinedException(factoryOptions));
  }


}
