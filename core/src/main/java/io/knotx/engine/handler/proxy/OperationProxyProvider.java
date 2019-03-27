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
package io.knotx.engine.handler.proxy;

import io.knotx.engine.api.proxy.CacheableProxy;
import io.knotx.engine.api.proxy.FragmentOperation;
import io.knotx.engine.api.proxy.OperationProxyFactory;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class OperationProxyProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperationProxyProvider.class);

  private final Map<String, OperationProxyFactoryOptions> options;
  private final Vertx vertx;

  private final Map<String, OperationProxyFactory> factories;
  private final Map<String, FragmentOperation> cache;

  public OperationProxyProvider(Map<String, OperationProxyFactoryOptions> options,
      Supplier<Iterator<OperationProxyFactory>> factoriesSupplier, Vertx vertx) {
    this.options = options;
    this.vertx = vertx;
    this.factories = loadFactories(factoriesSupplier);
    this.cache = new HashMap<>();
  }

  public Optional<FragmentOperation> get(String alias) {
    OperationProxyFactoryOptions config = options.get(alias);
    if (config == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing config.", alias);
      return Optional.empty();
    }
    OperationProxyFactory factory = factories.get(config.getFactory());
    if (factory == null) {
      LOGGER.warn("Could not create initialize proxy [{}] with missing factory [{}].", alias,
          config.getFactory());
      return Optional.empty();
    }

    // recurrence here :)
    FragmentOperation operation = config.getNext().flatMap(this::get).orElse(null);

    if (isCacheable(factory)) {
      return Optional.of(cache.computeIfAbsent(alias,
          a -> factory.create(alias, config.getConfig(), vertx, operation)));
    } else {
      return Optional.of(factory.create(alias, config.getConfig(), vertx, operation));
    }
  }

  private boolean isCacheable(OperationProxyFactory factory) {
    return factory.getClass().isAnnotationPresent(CacheableProxy.class);
  }

  private Map<String, OperationProxyFactory> loadFactories(
      Supplier<Iterator<OperationProxyFactory>> factoriesSupplier) {
    Map<String, OperationProxyFactory> result = new HashMap<>();
    factoriesSupplier.get().forEachRemaining(factory -> result.put(factory.getName(), factory));
    return result;
  }

}
