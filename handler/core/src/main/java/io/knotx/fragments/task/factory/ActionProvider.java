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
package io.knotx.fragments.task.factory;

import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

public class ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProvider.class);

  private final Map<String, ActionFactory> factories;
  private final Map<String, Action> cache;

  public ActionProvider(Supplier<Iterator<ActionFactory>> supplier) {
    this.factories = loadFactories(supplier);
    this.cache = new HashMap<>();
  }

  public Optional<Action> get(String action, Map<String, ActionOptions> actionNameToOptions,
      Vertx vertx) {
    if (StringUtils.isBlank(action)) {
      return Optional.empty();
    }
    ActionOptions actionOptions = actionNameToOptions.get(action);
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
      return Optional.of(cache.computeIfAbsent(action, toAction(actionOptions, factory, actionNameToOptions, vertx)));
    } else {
      return Optional.of(createAction(action, actionOptions, factory, actionNameToOptions, vertx));
    }
  }

  private Function<String, Action> toAction(ActionOptions actionOptions, ActionFactory factory,
      Map<String, ActionOptions> actionNameToOptions,
      Vertx vertx) {
    return action -> createAction(action, actionOptions, factory, actionNameToOptions, vertx);
  }

  private Action createAction(String action, ActionOptions actionOptions, ActionFactory factory,
      Map<String, ActionOptions> actionNameToOptions,
      Vertx vertx) {
    // recurrence here :)
    Action operation = Optional.ofNullable(actionOptions.getDoAction())
        .flatMap(actionName -> get(actionName, actionNameToOptions, vertx))
        .orElse(null);

    return factory.create(action, actionOptions.getConfig(), vertx.getDelegate(), operation);
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
