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
package io.knotx.fragments.action.library.cache.redis;

import io.knotx.commons.json.JsonParser;
import io.knotx.fragments.action.library.cache.Cache;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.reactivex.redis.client.Response;

public class RedisCache implements Cache {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisCache.class);

  private final Redis redisClient;

  private final RedisAPI redis;

  private final long ttlInSeconds;

  RedisCache(Redis redisClient, RedisCacheOptions options) {
    this.ttlInSeconds = options.getTtl();
    this.redisClient = redisClient;
    this.redis = RedisAPI.api(redisClient);
  }

  @Override
  public Maybe<Object> get(String key) {
    return redisClient.rxConnect()
        .flatMapMaybe(success -> redis.rxGet(key))
        .map(Response::toString)
        .map(JsonParser::parseIfJson);
  }

  @Override
  public void put(String key, Object value) {
    if (value instanceof JsonObject || value instanceof JsonArray || value instanceof String) {
      redis.rxSetex(key, Long.toString(ttlInSeconds), value.toString())
          .doOnSuccess(stuff -> LOGGER.info("New value cached under key: {} for {} seconds", key,
              ttlInSeconds))
          .doOnComplete(() -> LOGGER
              .warn("Received null response while caching new value under key: {}", key))
          .doOnError(
              error -> LOGGER.error("Error while caching new value under key: {}", error, key))
          .subscribe();
    } else {
      LOGGER.error(
          "Redis cache implementation supports only JsonObject, JsonArray and String values. "
              + "Received value: {} ({})", value, value.getClass());
    }
  }
}
