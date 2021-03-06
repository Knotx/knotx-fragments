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
package io.knotx.fragments.action.core;


import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Supplier;

/**
 * Action provider initializes {@link Action}, combines actions with behaviours and caches stateful
 * ones.
 */
public class ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProvider.class);

  private final Map<String, ActionFactory> factories;
  private final Map<String, Action> cache;

  private final Map<String, ActionFactoryOptions> aliasToOptions;
  private final Vertx vertx;

  public ActionProvider(Supplier<Iterable<ActionFactory>> supplier,
      Map<String, ActionFactoryOptions> aliasToOptions, Vertx vertx) {
    this.aliasToOptions = aliasToOptions;
    this.vertx = vertx;
    this.factories = loadFactories(supplier);
    this.cache = new HashMap<>();
  }

  /**
   * It provides an action instance by alias (action name) if configured.
   *
   * @param alias action alias / name
   * @return action instance if configured
   */
  public Optional<Action> get(String alias) {
    if (StringUtils.isBlank(alias)) {
      return Optional.empty();
    }
    ActionFactoryOptions actionFactoryOptions = aliasToOptions.get(alias);
    if (actionFactoryOptions == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing config.", alias);
      return Optional.empty();
    }
    ActionFactory factory = factories.get(actionFactoryOptions.getFactory());
    if (factory == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing factory [{}].", alias,
          actionFactoryOptions.getFactory());
      return Optional.empty();
    }

    if (isCacheable(factory)) {
      return Optional.ofNullable(cache.get(alias))
          .map(Optional::of)
          .orElseGet(() -> Optional.ofNullable(createAction(alias, actionFactoryOptions, factory)))
          .map(createdAction -> cacheIfAbsent(alias, createdAction));
    } else {
      return Optional.of(createAction(alias, actionFactoryOptions, factory));
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
      Supplier<Iterable<ActionFactory>> factoriesSupplier) {
    Map<String, ActionFactory> result = new HashMap<>();
    factoriesSupplier.get().forEach(factory -> result.put(factory.getName(), factory));
    LOGGER.debug("Action Factories: {}", result);
    return result;
  }

}
