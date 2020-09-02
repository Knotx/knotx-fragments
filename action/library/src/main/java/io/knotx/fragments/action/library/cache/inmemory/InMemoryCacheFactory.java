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
package io.knotx.fragments.action.library.cache.inmemory;

import io.knotx.fragments.action.library.cache.Cache;
import io.knotx.fragments.action.library.cache.CacheFactory;
import io.vertx.core.json.JsonObject;

public class InMemoryCacheFactory implements CacheFactory {

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL_MS = 5000;

  @Override
  public String getType() {
    return "in-memory";
  }

  @Override
  public Cache create(JsonObject config) {
    long maxSize =
        config == null ? DEFAULT_MAXIMUM_SIZE : config.getLong("maximumSize", DEFAULT_MAXIMUM_SIZE);
    long ttlMs = config == null ? DEFAULT_TTL_MS : config.getLong("ttlMs", DEFAULT_TTL_MS);
    // TODO: create data object for this
    return new InMemoryCache(maxSize, ttlMs);
  }

}
