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
package io.knotx.fragments.handler.action;

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.CONFIG_KEY_NAME;

import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

public class ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProvider.class);

  private final Map<String, ActionOptions> options;
  private final Vertx vertx;

  private final Map<String, ActionFactory> factories;
  private final Map<String, Action> cache;
  private final String globalActionLogLevel;

  public ActionProvider(Map<String, ActionOptions> options,
      Supplier<Iterator<ActionFactory>> factoriesSupplier, String globalActionLogLevel, Vertx vertx) {
    this.options = options;
    this.vertx = vertx;
    this.factories = loadFactories(factoriesSupplier);
    this.globalActionLogLevel = globalActionLogLevel;
    this.cache = new HashMap<>();
  }

  public Optional<Action> get(String action) {
    if (StringUtils.isBlank(action)) {
      return Optional.empty();
    }
    ActionOptions actionOptions = options.get(action);
    if (actionOptions == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing config.", action);
      return Optional.empty();
    }
    ActionFactory factory = factories.get(actionOptions.getFactory());
    if (factory == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing factory [{}].", action,
          actionOptions.getFactory());
      return Optional.empty();
    }

    if (isCacheable(factory)) {
      return Optional.of(cache.computeIfAbsent(action, toAction(actionOptions, factory)));
    } else {
      return Optional.of(createAction(action, actionOptions, factory));
    }
  }

  private Function<String, Action> toAction(ActionOptions actionOptions, ActionFactory factory) {
    return action -> createAction(action, actionOptions, factory);
  }

  private Action createAction(String action, ActionOptions actionOptions, ActionFactory factory){
    // recurrence here :)
    Action operation = Optional.ofNullable(actionOptions.getDoAction())
        .flatMap(this::get)
        .orElse(null);

    return factory.create(action, prepareActionConfig(actionOptions), vertx, operation);
  }

  private JsonObject prepareActionConfig(ActionOptions actionOptions){
    JsonObject config = actionOptions.getConfig();

    if(config.fieldNames().contains(CONFIG_KEY_NAME)){
      return config;
    }

    return config.put(CONFIG_KEY_NAME, globalActionLogLevel);
  }

  private boolean isCacheable(ActionFactory factory) {
    return factory.getClass().isAnnotationPresent(Cacheable.class);
  }

  private Map<String, ActionFactory> loadFactories(
      Supplier<Iterator<ActionFactory>> factoriesSupplier) {
    Map<String, ActionFactory> result = new HashMap<>();
    factoriesSupplier.get().forEachRemaining(factory -> result.put(factory.getName(), factory));
    LOGGER.debug("Action Factories: {}", result);
    return result;
  }

}
