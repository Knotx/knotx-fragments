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
package io.knotx.fragments.task.factory.node.action;


import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.task.factory.ActionFactoryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

public class ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProvider.class);

  private final Map<String, ActionFactory> factories;
  private final Map<String, Action> cache;

  private Map<String, ActionFactoryOptions> actionNameToOptions;
  private Vertx vertx;

  public ActionProvider(Supplier<Iterator<ActionFactory>> supplier,
      Map<String, ActionFactoryOptions> actionNameToOptions, Vertx vertx) {
    this.actionNameToOptions = actionNameToOptions;
    this.vertx = vertx;
    this.factories = loadFactories(supplier);
    this.cache = new HashMap<>();
  }

  public Optional<Action> get(String action) {
    if (StringUtils.isBlank(action)) {
      return Optional.empty();
    }
    ActionFactoryOptions actionFactoryOptions = actionNameToOptions.get(action);
    if (actionFactoryOptions == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing config.", action);
      return Optional.empty();
    }
    ActionFactory factory = factories.get(actionFactoryOptions.getFactory());
    if (factory == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing factory [{}].", action,
          actionFactoryOptions.getFactory());
      return Optional.empty();
    }

    if (isCacheable(factory)) {
      return Optional.ofNullable(cache.get(action))
          .map(Optional::of)
          .orElseGet(() -> Optional.ofNullable(createAction(action, actionFactoryOptions, factory)))
          .map(createdAction -> cacheIfAbsent(action, createdAction));
    } else {
      return Optional.of(createAction(action, actionFactoryOptions, factory));
    }
  }

  private Action cacheIfAbsent(String key, Action action) {
    cache.putIfAbsent(key, action);
    return action;
  }

  private Action createAction(String action, ActionFactoryOptions actionFactoryOptions,
      ActionFactory factory) {
    // recurrence here :)
    Action operation = Optional.ofNullable(actionFactoryOptions.getDoAction())
        .flatMap(this::get)
        .orElse(null);

    return factory.create(action, actionFactoryOptions.getConfig(), vertx.getDelegate(), operation);
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
