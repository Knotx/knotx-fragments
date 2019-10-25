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


import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionConfig;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

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

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL = 5000;


  @Override
  public String getName() {
    return "in-memory-cache";
  }

  @Override
  public Action create(ActionConfig config, Vertx vertx) {

    return new Action() {
      JsonObject options = config.getOptions();
      Action doAction = config.getDoAction();
      ActionLogger actionLogger = ActionLogger.create(config);
      private Cache<String, Object> cache = createCache(options);
      private String payloadKey = getPayloadKey(options);

      @Override
      public void apply(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler) {
        String cacheKey = getCacheKey(options, fragmentContext.getClientRequest());
        Object cachedValue = cache.getIfPresent(cacheKey);
        actionLogger.info("cached_key", cacheKey);
        if (cachedValue == null) {
          callDoActionAndCache(fragmentContext, resultHandler, cacheKey);
        } else {
          actionLogger.info("cached_value", cachedValue);
          Fragment fragment = fragmentContext.getFragment();
          fragment.appendPayload(payloadKey, cachedValue);
          FragmentResult result = new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION,
              actionLogger.toLog());
          Future.succeededFuture(result)
              .setHandler(resultHandler);
        }
      }

      private void callDoActionAndCache(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler, String cacheKey) {
        doAction.apply(fragmentContext, asyncResult -> {
          if (asyncResult.succeeded()) {
            FragmentResult fragmentResult = asyncResult.result();
            if (FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition())
                && fragmentResult.getFragment()
                .getPayload()
                .containsKey(payloadKey)) {
              JsonObject resultPayload = fragmentResult.getFragment()
                  .getPayload();
              Object cachedValue = resultPayload.getMap().get(payloadKey);
              actionLogger.info("new_cached_value", cachedValue);
              cache.put(cacheKey, cachedValue);
            }
            actionLogger.doActionLog(asyncResult.result().getActionLog());
            Future.succeededFuture(toResult(fragmentResult))
                .setHandler(resultHandler);
          } else {
            Future.<FragmentResult>failedFuture(asyncResult.cause()).setHandler(resultHandler);
          }
        });
      }

      private FragmentResult toResult(FragmentResult fragmentResult) {
        JsonObject actionLog = Objects.isNull(fragmentResult.getActionLog()) ? actionLogger.toLog()
            : fragmentResult.getActionLog().mergeIn(actionLogger.toLog());
        return new FragmentResult(fragmentResult.getFragment(), fragmentResult.getTransition(),
            actionLog);
      }
    };
  }

  private String getPayloadKey(JsonObject options) {
    String result = options.getString("payloadKey");
    if (StringUtils.isBlank(result)) {
      throw new IllegalArgumentException(
          "Action requires payloadKey value in configuration.");
    }
    return result;
  }

  private String getCacheKey(JsonObject options, ClientRequest clientRequest) {
    String key = options.getString("cacheKey");
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

  private Cache<String, Object> createCache(JsonObject options) {
    JsonObject cache = options.getJsonObject("cache");
    long maxSize =
        cache == null ? DEFAULT_MAXIMUM_SIZE : cache.getLong("maximumSize", DEFAULT_MAXIMUM_SIZE);
    long ttl = cache == null ? DEFAULT_TTL : cache.getLong("ttl", DEFAULT_TTL);
    return CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
        .build();
  }
}
