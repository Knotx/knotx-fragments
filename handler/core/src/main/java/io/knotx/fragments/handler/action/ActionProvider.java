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

import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

public class ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProvider.class);

  private final Map<String, ActionOptions> options;
  private final Vertx vertx;

  private final Map<String, ActionFactory> factories;
  private final Map<String, Action> cache;

  public ActionProvider(Map<String, ActionOptions> options,
      Supplier<Iterator<ActionFactory>> factoriesSupplier, Vertx vertx) {
    this.options = options;
    this.vertx = vertx;
    this.factories = loadFactories(factoriesSupplier);
    this.cache = new HashMap<>();
  }

  public Optional<Action> get(String action) {
    if (StringUtils.isBlank(action)) {
      return Optional.empty();
    }
    ActionOptions config = options.get(action);
    if (config == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing config.", action);
      return Optional.empty();
    }
    ActionFactory factory = factories.get(config.getFactory());
    if (factory == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing factory [{}].", action,
          config.getFactory());
      return Optional.empty();
    }

    // recurrence here :)
    Action operation = Optional.ofNullable(config.getDoAction())
        .flatMap(this::get)
        .orElse(null);

    if (isCacheable(factory)) {
      return Optional.of(cache.computeIfAbsent(action,
          a -> factory.create(a, config.getConfig(), vertx, operation)));
    } else {
      return Optional.of(factory.create(action, config.getConfig(), vertx, operation));
    }
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
