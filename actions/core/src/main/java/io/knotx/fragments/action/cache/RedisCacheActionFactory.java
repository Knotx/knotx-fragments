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

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Action factory for caching fragment payload values on Redis server. Can be initialized with a
 * configuration:
 * <pre>
 *   productDetails {
 *     factory = redis-cache
 *     config {
 *       redis {
 *         host = localhost
 *         port = 6379
 *         password = my-password
 *       }
 *       cache.ttl = 60
 *       cacheKey = product-{param.id}
 *       payloadKey = product
 *     }
 *     doAction = fetch-product
 *   }
 * </pre>
 *
 * Parameters:
 * <pre>
 *   redis.host - default value: "localhost"
 *   redis.port - default value: 6379
 *   redis.password - empty by default
 *   cache.ttl - in seconds, default value: 60
 * </pre>
 */
public class RedisCacheActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "redis-cache";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    return new CacheAction(doAction, config, new RedisCache(vertx, config));
  }
}
