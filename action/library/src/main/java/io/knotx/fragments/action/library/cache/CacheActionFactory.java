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
package io.knotx.fragments.action.library.cache;

import com.google.common.collect.ImmutableList;
import io.knotx.commons.cache.Cache;
import io.knotx.commons.cache.CacheFactory;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.ServiceLoader;

@Cacheable
public class CacheActionFactory implements ActionFactory {

  private static final List<CacheFactory> factories = ImmutableList
      .copyOf(ServiceLoader.load(CacheFactory.class));

  @Override
  public String getName() {
    return "cache";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    CacheActionOptions options = new CacheActionOptions(config);
    Cache cache = createCache(alias, options);

    return new CacheAction(cache, options, alias, doAction);
  }

  private Cache createCache(String alias, CacheActionOptions options) {
    CacheFactory factory = factories.stream()
        .filter(f -> f.getType().equals(options.getType()))
        .findFirst()
        .orElseThrow(() -> new ActionConfigurationException(alias, String
            .format("Requested CacheFactory of type [%s] not found via ServiceLoader (SPI)",
                options.getType())));
    return factory.create(options.getCache());
  }
}
