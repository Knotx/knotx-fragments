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
package io.knotx.fragments.handler.action.cache;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

public class CacheAction implements Action {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheAction.class);

  private final Action doAction;

  private final String payloadKey;

  private final JsonObject config;

  private final Cache cache;

  public CacheAction(Action doAction, JsonObject config,
      Cache cache) {
    this.doAction = doAction;
    this.config = config;
    this.payloadKey = getPayloadKey(config);
    this.cache = cache;
  }

  @Override
  public void apply(FragmentContext fragmentContext, Handler<AsyncResult<FragmentResult>> resultHandler) {
    String cacheKey = getCacheKey(config, fragmentContext.getClientRequest());
    cache.get(cacheKey)
        .subscribe(cachedValue -> {
          LOGGER.trace("Cache responsed with value: {}", cachedValue);
          Fragment fragment = fragmentContext.getFragment();
          fragment.appendPayload(payloadKey, cachedValue);
          FragmentResult result = new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
          Future.succeededFuture(result)
              .setHandler(resultHandler);
        }, error -> {
          LOGGER.error("Could not fetch data from cache. {}", error);
          Fragment fragment = fragmentContext.getFragment();
          FragmentResult result = new FragmentResult(fragment, FragmentResult.ERROR_TRANSITION);
          Future.succeededFuture(result)
              .setHandler(resultHandler);
        }, () -> {
          LOGGER.trace("Cache didn't return value.");
          callDoActionAndCache(fragmentContext, resultHandler, cacheKey);
        });
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
          cache.put(cacheKey, resultPayload.getMap()
              .get(payloadKey));
        }
        Future.succeededFuture(fragmentResult)
            .setHandler(resultHandler);
      } else {
        Future.<FragmentResult>failedFuture(asyncResult.cause()).setHandler(resultHandler);
      }
    });
  }

  private static String getPayloadKey(JsonObject config) {
    String result = config.getString("payloadKey");
    if (StringUtils.isBlank(result)) {
      throw new IllegalArgumentException(
          "Action requires payloadKey value in configuration.");
    }
    return result;
  }

  private static String getCacheKey(JsonObject config, ClientRequest clientRequest) {
    String key = config.getString("cacheKey");
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Action requires cacheKey value in configuration.");
    }
    return PlaceholdersResolver.resolve(key, buildSourceDefinitions(clientRequest));
  }

  private static SourceDefinitions buildSourceDefinitions(ClientRequest clientRequest) {
    return SourceDefinitions.builder()
        .addClientRequestSource(clientRequest)
        .build();
  }
}
