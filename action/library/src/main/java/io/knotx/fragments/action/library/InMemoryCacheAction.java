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

import static io.knotx.fragments.action.library.helper.ValidationHelper.checkArgument;
import static io.knotx.fragments.api.FragmentResult.fail;
import static io.knotx.fragments.api.FragmentResult.success;

import com.google.common.cache.Cache;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.SingleAction;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class InMemoryCacheAction implements SingleAction {

  public static final String CACHE_KEY = "cache_key";
  public static final String CACHED_VALUE = "cached_value";
  public static final String COMPUTED_VALUE = "computed_value";
  public static final String CACHE_MISS = "cache_miss";
  public static final String CACHE_HIT = "cache_hit";
  public static final String CACHE_PASS = "cache_pass";

  private final Cache<String, Object> cache;
  private final String payloadKey;
  private final ActionLogLevel logLevel;
  private final Action doAction;
  private final JsonObject config;

  public InMemoryCacheAction(Cache<String, Object> cache, String payloadKey,
      ActionLogLevel logLevel, Action doAction, JsonObject config) {
    this.cache = cache;
    this.payloadKey = payloadKey;
    this.logLevel = logLevel;
    this.doAction = doAction;
    this.config = config;
  }

  @Override
  public Single<FragmentResult> apply(FragmentContext fragmentContext) {
    CacheActionLogger logger = CacheActionLogger.create("Test", logLevel); // TODO
    final String cacheKey = getCacheKey(config, fragmentContext.getClientRequest());
    logger.onLookup(cacheKey);

    return getFromCache(fragmentContext, cacheKey, logger)
        .switchIfEmpty(callDoActionAndCache(fragmentContext, cacheKey, logger))
        .doOnError(logger::onError)
        // all errors are transformed to _error transition
        .onErrorReturn(error -> handleFailure(fragmentContext, error, logger));
  }

  private FragmentResult handleFailure(FragmentContext fragmentContext, Throwable error,
      CacheActionLogger logger) {
    return fail(fragmentContext.getFragment(), logger.getLogAsJson(), error);
  }

  private Maybe<FragmentResult> getFromCache(FragmentContext fragmentContext, String cacheKey,
      CacheActionLogger logger) {
    return Maybe.just(cacheKey)
        .flatMap(this::findInCache)
        .doOnSuccess(logger::onHit)
        .map(cachedValue -> fragmentContext.getFragment()
            .appendPayload(payloadKey, cachedValue))
        .map(fragment -> toResultWithLog(logger, fragment));
  }

  private String getCacheKey(JsonObject config, ClientRequest clientRequest) {
    String key = config.getString("cacheKey");
    checkArgument("TODO", StringUtils.isBlank(key), // TODO
        "Action requires cacheKey value in configuration.");
    return PlaceholdersResolver.resolveAndEncode(key, buildSourceDefinitions(clientRequest));
  }

  private SourceDefinitions buildSourceDefinitions(ClientRequest clientRequest) {
    return SourceDefinitions.builder()
        .addClientRequestSource(clientRequest)
        .build();
  }

  private Maybe<Object> findInCache(String key) {
    Object cachedValue = cache.getIfPresent(key);
    if (cachedValue == null) {
      return Maybe.empty();
    } else {
      return Maybe.just(cachedValue);
    }
  }

  private Single<FragmentResult> callDoActionAndCache(FragmentContext fragmentContext,
      String cacheKey,
      CacheActionLogger actionLogger) {
    actionLogger.onRetrieveStart();
    return FragmentOperation.newInstance(doAction)
        .rxApply(fragmentContext)
        .doOnSuccess(actionLogger::onRetrieveEnd)
        .doOnSuccess(fr -> savePayloadToCache(actionLogger, cacheKey, fr))
        .map(fr -> toResultWithLog(actionLogger, fr));
  }

  private void savePayloadToCache(CacheActionLogger logger, String cacheKey,
      FragmentResult fragmentResult) {
    if (isCacheable(fragmentResult)) {
      Object resultPayload = getAppendedPayload(fragmentResult);
      cache.put(cacheKey, resultPayload);
      logger.onMiss(resultPayload);
    } else {
      logger.onPass();
    }
  }

  private boolean isCacheable(FragmentResult fragmentResult) {
    return isSuccessTransition(fragmentResult)
        && fragmentResult.getFragment()
        .getPayload()
        .containsKey(payloadKey);
  }

  private Object getAppendedPayload(FragmentResult fragmentResult) {
    return fragmentResult.getFragment()
        .getPayload().getMap().get(payloadKey);
  }

  private FragmentResult toResultWithLog(CacheActionLogger logger, Fragment fragment) {
    return success(fragment, logger.getLogAsJson());
  }

  private FragmentResult toResultWithLog(CacheActionLogger logger,
      FragmentResult fragmentResult) {
    return success(fragmentResult.getFragment(), fragmentResult.getTransition(),
        logger.getLogAsJson());
  }

  private static boolean isSuccessTransition(FragmentResult fragmentResult) {
    return FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition());
  }

}
