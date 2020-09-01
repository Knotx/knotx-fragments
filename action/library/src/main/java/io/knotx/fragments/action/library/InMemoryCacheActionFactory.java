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
package io.knotx.fragments.action.library;


import static io.knotx.fragments.action.api.log.ActionLogLevel.fromConfig;
import static io.knotx.fragments.action.library.helper.ValidationHelper.checkArgument;
import static io.knotx.fragments.api.FragmentResult.success;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.knotx.fragments.action.api.SingleAction;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.action.library.helper.TimeCalculator;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

/**
 * Payload Cache Action factory class. It can be initialized with a configuration:
 * <pre>
 *   productDetails {
 *     name = in-memory-cache,
 *     config {
 *       cache {
 *         maximumSize = 1000
 *         ttl = 5000
 *       }
 *       cacheKey = product-{param.id}
 *       payloadKey = product
 *       logLevel = error
 *     }
 *   }
 * </pre>
 */
@Cacheable
public class InMemoryCacheActionFactory implements ActionFactory {

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL = 5000;

  @Override
  public String getName() {
    return "in-memory-cache";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    final Cache<String, Object> cache = createCache(config);
    final String payloadKey = getPayloadKey(config);
    final ActionLogLevel logLevel = fromConfig(config, ActionLogLevel.ERROR);

    return new InMemoryCacheAction(cache, payloadKey, logLevel, doAction, config);
  }

  private String getPayloadKey(JsonObject config) {
    String result = config.getString("payloadKey");
    checkArgument(getName(), StringUtils.isBlank(result),
        "Action requires payloadKey value in configuration.");
    return result;
  }

  private Cache<String, Object> createCache(JsonObject config) {
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
