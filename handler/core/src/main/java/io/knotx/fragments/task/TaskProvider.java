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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class TaskProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskProvider.class);

  private final String taskKey;
  private final Map<String, TaskFactory> factories;
  private final JsonArray factoryOptions;
  private final Vertx vertx;

  private List<TaskFactory> orderedFactories;

  public TaskProvider(String taskKey, JsonArray factoryOptions, Vertx vertx) {
    this.taskKey = taskKey;
    this.factoryOptions = factoryOptions;
    factories = initFactories();
    orderedFactories = init(factoryOptions);
    this.vertx = vertx;
  }

  public Optional<Task> newInstance(FragmentEventContext eventContext) {
    return orderedFactories.stream()
        .filter(f -> f.accept(eventContext))
        .findFirst()
        .map(f -> f.newInstance(eventContext, get(f.getName()), vertx));
  }

  private JsonObject get(String name) {
    JsonObject config = new JsonObject();
    Iterator<Object> iterator = factoryOptions.iterator();
    while(iterator.hasNext()) {
      JsonObject options = (JsonObject) iterator.next();
      String factory = options.getString("name");
      if (name.equals(factory)) {
        config = options.getJsonObject("config");
        break;
      }
    }
    return config;
  }

  private String getTaskName(Fragment fragment) {
    return fragment.getConfiguration().getString(taskKey);
  }

  private boolean hasTask(Fragment fragment) {
    return fragment.getConfiguration().containsKey(taskKey);
  }

  List<TaskFactory> init(JsonArray factoryOptions) {
    List<TaskFactory> orderedFactories = new ArrayList<>();
    factoryOptions.iterator().forEachRemaining(options -> {
      String factory = ((JsonObject) options).getString("name");
      JsonObject config = ((JsonObject) options).getJsonObject("config");
      orderedFactories.add(factories.get(factory));
    });
    return orderedFactories;
  }

  private Map<String, TaskFactory> initFactories() {
    Map<String, TaskFactory> factories = new HashMap<>();
    ServiceLoader
        .load(TaskFactory.class).iterator()
        .forEachRemaining(f -> factories.put(f.getName(), f));
    return factories;
  }

}
