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


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.helper.TimeCalculator;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
 *     }
 *   }
 * </pre>
 */
@Cacheable
public class InMemoryCacheActionFactory implements ActionFactory {

  public static final String CACHE_LOOKUP = "CACHE_LOOKUP";
  public static final String CACHE_MISS = "CACHE_MISS";
  public static final String CACHE_HIT = "CACHE_HIT";
  public static final String CACHE_PASS = "CACHE_PASS";
  public static final String TRANSITION = "TRANSITION";

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL = 5000;


  @Override
  public String getName() {
    return "in-memory-cache";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {

    return new Action() {
      private Cache<String, Object> cache = createCache(config);
      private String payloadKey = getPayloadKey(config);

      @Override
      public void apply(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler) {

        String cacheKey = getCacheKey(config, fragmentContext.getClientRequest());

        ActionLogger actionLogger = ActionLogger.create(alias, ActionLogLevel.INFO);
        logCacheLookup(actionLogger, cacheKey);

        Object cachedValue = cache.getIfPresent(cacheKey);
        if (cachedValue == null) {
          callDoActionAndCache(fragmentContext, resultHandler, cacheKey, actionLogger);
        } else {
          logCacheHit(actionLogger, cachedValue);
          Fragment fragment = fragmentContext.getFragment();
          fragment.appendPayload(payloadKey, cachedValue);
          FragmentResult result = new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION,
              actionLogger.toLog().toJson());
          Future.succeededFuture(result)
              .setHandler(resultHandler);
        }
      }

      private void callDoActionAndCache(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler, String cacheKey,
          ActionLogger actionLogger) {
        long startTime = Instant.now().toEpochMilli();
        doAction.apply(fragmentContext, asyncResult -> {
          if (asyncResult.succeeded()) {
            FragmentResult fragmentResult = asyncResult.result();
            actionLogger
                .doActionLog(TimeCalculator.executionTime(startTime), fragmentResult.getNodeLog());
            if (FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition())
                && fragmentResult.getFragment()
                .getPayload()
                .containsKey(payloadKey)) {
              Object resultPayload = fragmentResult.getFragment()
                  .getPayload().getMap().get(payloadKey);
              logCacheMiss(actionLogger, resultPayload);
              cache.put(cacheKey, resultPayload);
            } else {
              logCachePass(actionLogger, fragmentResult);
            }
            Future.succeededFuture(
                new FragmentResult(fragmentResult.getFragment(), fragmentResult.getTransition(),
                    actionLogger.toLog().toJson()))
                .setHandler(resultHandler);
          } else {
            Future.<FragmentResult>failedFuture(asyncResult.cause()).setHandler(resultHandler);
          }
        });
      }
    };
  }

  private String getPayloadKey(JsonObject config) {
    String result = config.getString("payloadKey");
    if (StringUtils.isBlank(result)) {
      throw new IllegalArgumentException(
          "Action requires payloadKey value in configuration.");
    }
    return result;
  }

  private String getCacheKey(JsonObject config, ClientRequest clientRequest) {
    String key = config.getString("cacheKey");
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Action requires cacheKey value in configuration.");
    }
    return PlaceholdersResolver.resolve(key, buildSourceDefinitions(clientRequest));
  }

  private SourceDefinitions buildSourceDefinitions(ClientRequest clientRequest) {
    return SourceDefinitions.builder()
        .addClientRequestSource(clientRequest)
        .build();
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

  private static void logCacheLookup(ActionLogger actionLogger, String cacheKey) {
    actionLogger.info(CACHE_LOOKUP, cacheKey);
  }

  private static void logCacheHit(ActionLogger actionLogger, Object cachedValue) {
    actionLogger.info(CACHE_HIT, cachedValue);
  }

  private static void logCacheMiss(ActionLogger actionLogger, Object computedValue) {
    actionLogger.info(CACHE_MISS, computedValue);
  }

  private static void logCachePass(ActionLogger actionLogger, FragmentResult failedResult) {
    actionLogger.info(CACHE_PASS,
        new JsonObject().put(TRANSITION, failedResult.getTransition()));
  }
}
