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
package io.knotx.fragments.task.provider;

import static io.knotx.fragments.task.provider.LocalTaskProviderFactoryOptions.NODE_LOG_LEVEL_KEY;

import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.task.TaskProvider;
import io.knotx.fragments.task.TaskProviderFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public class LocalTaskProviderFactory implements TaskProviderFactory {

  public static final String NAME = "configuration";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public TaskProvider create(JsonObject config, Vertx vertx) {
    LocalTaskProviderFactoryOptions options = new LocalTaskProviderFactoryOptions(config);

    Map<String, ActionOptions> actions = options.getActions();
    prepareActionConfig(actions, options.getLogLevel());

    ActionProvider actionProvider = new ActionProvider(actions, supplyFactories(),
        vertx.getDelegate());
    return new LocalTaskProvider(actionProvider);
  }

  private Supplier<Iterator<ActionFactory>> supplyFactories() {
    return () -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader
          .load(ActionFactory.class);
      return factories.iterator();
    };
  }

  private void prepareActionConfig(Map<String, ActionOptions> actionOptions,
      String globalActionLogLevel) {
    actionOptions.values().stream()
        .map(options -> {
          JsonObject config = options.getConfig();
          if (config.fieldNames().contains(NODE_LOG_LEVEL_KEY)) {
            return config;
          }
          return config.put(NODE_LOG_LEVEL_KEY, globalActionLogLevel);
        });
  }
}
