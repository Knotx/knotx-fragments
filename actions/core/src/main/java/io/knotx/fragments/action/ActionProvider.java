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
package io.knotx.fragments.action;


import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;

/**
 * Action provider initializes {@link Action}, combines actions with behaviours and caches stateful
 * ones.
 */
public class ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProvider.class);

  private final Map<String, ActionFactory> factories;
  private final Map<String, Action> cache;

  private Map<String, ActionFactoryOptions> aliasToOptions;
  private Vertx vertx;

  /**
   * It gets a list of {@link ActionFactory} implementations as a parameter.
   */
  public ActionProvider(Supplier<Iterator<ActionFactory>> supplier,
      Map<String, ActionFactoryOptions> aliasToOptions, Vertx vertx) {
    this.aliasToOptions = aliasToOptions;
    this.vertx = vertx;
    this.factories = loadFactories(supplier);
    this.cache = new ConcurrentHashMap<>();
  }

  /**
   * I uses <a href="https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html">Java Service
   * Provider Interface</a> to load {@link ActionFactory} implementations available on the
   * classpath.
   */
  public ActionProvider(Map<String, ActionFactoryOptions> actionNameToOptions, Vertx vertx) {
    this(() -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader.load(ActionFactory.class);
      return factories.iterator();
    }, actionNameToOptions, vertx);
  }

  /**
   * It provides action instance by alias.
   *
   * @param alias action alias
   * @return action instance
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
      return Optional.of(cache.computeIfAbsent(alias, toAction(actionFactoryOptions, factory)));
    } else {
      return Optional.of(createAction(alias, actionFactoryOptions, factory));
    }
  }

  private Function<String, Action> toAction(ActionFactoryOptions actionFactoryOptions,
      ActionFactory factory) {
    return action -> createAction(action, actionFactoryOptions, factory);
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
