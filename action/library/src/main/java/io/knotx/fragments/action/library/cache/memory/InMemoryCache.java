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
package io.knotx.fragments.action.library.cache.memory;


import com.google.common.cache.CacheBuilder;
import io.knotx.fragments.action.library.cache.Cache;
import io.reactivex.Maybe;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class InMemoryCache implements Cache {

  private final com.google.common.cache.Cache<String, Object> cache;

  InMemoryCache(InMemoryCacheOptions options) {
    cache = createCache(options);
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

  private static com.google.common.cache.Cache<String, Object> createCache(
      InMemoryCacheOptions options) {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    if (options.getMaximumSize() != null) {
      builder.maximumSize(options.getMaximumSize());
    }
    if (options.getTtl() != null) {
      builder.expireAfterWrite(options.getTtl(), TimeUnit.MILLISECONDS);
    }
    if (options.getTtlAfterAccess() != null) {
      builder.expireAfterAccess(options.getTtlAfterAccess(), TimeUnit.MILLISECONDS);
    }
    return builder.build();
  }
}
