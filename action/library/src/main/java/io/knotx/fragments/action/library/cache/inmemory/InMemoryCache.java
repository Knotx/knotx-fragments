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

import com.google.common.cache.CacheBuilder;
import io.knotx.fragments.action.library.cache.Cache;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.TimeUnit;

public class InMemoryCache implements Cache {

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL = 5000;

  private final com.google.common.cache.Cache<String, Object> cache;

  public InMemoryCache(JsonObject config) {
    cache = createCache(config);
  }

  @Override
  public Maybe<Object> get(String key) {
    Object cachedValue = cache.getIfPresent(key);
    if (cachedValue == null) {
      return Maybe.empty();
    } else {
      return Maybe.just(cachedValue);
    }
  }

  @Override
  public void put(String key, Object value) {
    cache.put(key, value);
  }

  private com.google.common.cache.Cache<String, Object> createCache(JsonObject config) {
    long maxSize =
        config == null ? DEFAULT_MAXIMUM_SIZE : config.getLong("maximumSize", DEFAULT_MAXIMUM_SIZE);
    long ttl = config == null ? DEFAULT_TTL : config.getLong("ttl", DEFAULT_TTL);
    return CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
        .build();
  }
}
