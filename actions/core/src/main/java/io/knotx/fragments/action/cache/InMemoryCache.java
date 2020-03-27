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
package io.knotx.fragments.action.cache;


import io.reactivex.Maybe;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;

import io.vertx.core.json.JsonObject;

public class InMemoryCache implements Cache {

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL = 5000;

  private final com.google.common.cache.Cache cache;

  InMemoryCache(JsonObject config) {
    cache = createCache(config);
  }

  @Override
  public Maybe<Object> get(String key) {
    return Optional.ofNullable(cache.getIfPresent(key))
        .map(Maybe::just)
        .orElse(Maybe.empty());
  }

  @Override
  public void put(String key, Object value) {
    cache.put(key, value);
  }

  private static com.google.common.cache.Cache createCache(JsonObject config) {
    JsonObject cache = config.getJsonObject("cache");
    long maxSize =
        cache == null ? DEFAULT_MAXIMUM_SIZE : cache.getLong("maximumSize", DEFAULT_MAXIMUM_SIZE);
    long ttl = cache == null ? DEFAULT_TTL : cache.getLong("ttl", DEFAULT_TTL);
    return CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
        .build();
  }
}
