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
import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.UriTransformer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
        Object cachedValue = cache.getIfPresent(cacheKey);
        if (cachedValue == null) {
          callDoActionAndCache(fragmentContext, resultHandler, cacheKey);
        } else {
          Fragment fragment = fragmentContext.getFragment();
          fragment.appendPayload(payloadKey, cachedValue);
          FragmentResult result = new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
          Future.succeededFuture(result).setHandler(resultHandler);
        }
      }

      private void callDoActionAndCache(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler, String cacheKey) {
        doAction.apply(fragmentContext, asyncResult -> {
          if (asyncResult.succeeded()) {
            FragmentResult fragmentResult = asyncResult.result();
            if (FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition())
                && fragmentResult.getFragment().getPayload().containsKey(payloadKey)) {
              JsonObject resultPayload = fragmentResult.getFragment().getPayload();
              cache.put(cacheKey, resultPayload.getMap().get(payloadKey));
            }
            Future.succeededFuture(fragmentResult).setHandler(resultHandler);
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
          "Action requires doActionPayloadKey value in configuration.");
    }
    return result;
  }

  private String getCacheKey(JsonObject config, ClientRequest clientRequest) {
    String key = config.getString("cacheKey");
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Action requires cacheKey value in configuration.");
    }
    return UriTransformer.resolveServicePath(key, clientRequest);
  }

  private Cache<String, Object> createCache(JsonObject config) {
    JsonObject cache = config.getJsonObject("cache");
    long maxSize =
        cache == null ? DEFAULT_MAXIMUM_SIZE : cache.getLong("maximumSize", DEFAULT_MAXIMUM_SIZE);
    long ttl = cache == null ? DEFAULT_TTL : cache.getLong("ttl", DEFAULT_TTL);
    return CacheBuilder.newBuilder().maximumSize(maxSize)
        .expireAfterWrite(ttl, TimeUnit.MILLISECONDS).build();
  }
}
