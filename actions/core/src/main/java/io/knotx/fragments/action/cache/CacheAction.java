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
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.action.helper.FragmentContextPlaceholderResolver;
import io.knotx.fragments.action.helper.TimeCalculator;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class CacheAction implements Action {

  private static final String CACHE_KEY = "cache_key";
  private static final String CACHED_VALUE = "cached_value";
  private static final String COMPUTED_VALUE = "computed_value";
  private static final String CACHE_MISS = "cache_miss";
  private static final String CACHE_HIT = "cache_hit";
  private static final String CACHE_PASS = "cache_pass";
  private static final String CACHE_GET_FAILURE = "cache_get_failure";
  private static final String CACHE_PUT_FAILURE = "cache_put_failure";

  private final String alias;

  private final Action doAction;

  private final String payloadKey;

  private final String cacheKeySchema;

  private final boolean failWhenCacheGetFails;

  private final boolean failWhenCachePutFails;

  private final Cache cache;

  private ActionLogLevel logLevel;

  public CacheAction(String alias, Action doAction, CacheOptions options,
      Cache cache) {
    this.alias = alias;
    this.doAction = doAction;
    this.cache = cache;
    this.payloadKey = notEmptyPayloadKey(options);
    this.cacheKeySchema = notEmptyCacheKey(options);
    this.failWhenCacheGetFails = options.isFailWhenCacheGetFails();
    this.failWhenCachePutFails = options.isFailWhenCachePutFails();
    this.logLevel = ActionLogLevel.fromConfig(options.getLogLevel());
  }

  @Override
  public void apply(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create(alias, logLevel);
    process(fragmentContext, actionLogger)
        .doOnError(actionLogger::error)
        .onErrorReturn(error -> toErrorTransition(fragmentContext))
        .map(result -> result.setLog(actionLogger.toLog().toJson()))
        .map(Future::succeededFuture)
        .doOnSuccess(future -> future.setHandler(resultHandler))
        .subscribe();
  }

  private Single<FragmentResult> process(FragmentContext fragmentContext,
      ActionLogger actionLogger) {
    String cacheKey = computeCacheKey(fragmentContext);
    return lookupInCache(cacheKey, fragmentContext, actionLogger)
        .switchIfEmpty(callDoAction(fragmentContext, actionLogger)
            .doOnSuccess(fr -> savePayloadToCache(cacheKey, fr, actionLogger)));
  }

  private Maybe<FragmentResult> lookupInCache(String cacheKey, FragmentContext fragmentContext,
      ActionLogger actionLogger) {
    return safeGet(cacheKey)
        .doOnSuccess(cachedValue -> logCacheHit(actionLogger, cacheKey, cachedValue))
        .map(cachedValue -> fragmentContext.getFragment().appendPayload(payloadKey, cachedValue))
        .map(this::toSuccessTransition)
        .doOnError(e -> logCacheGetFailure(actionLogger, cacheKey, e))
        .onErrorResumeNext(e -> failWhenCacheGetFails ? Maybe.error(e) : Maybe.empty());
  }

  private Maybe<Object> safeGet(String cacheKey) {
    try {
      return Objects.requireNonNull(cache.get(cacheKey));
    } catch (Exception e) {
      return Maybe.error(e);
    }
  }

  private Single<FragmentResult> callDoAction(FragmentContext fragmentContext,
      ActionLogger actionLogger) {
    long startTime = Instant.now().toEpochMilli();
    return FragmentOperation.newInstance(doAction)
        .rxApply(fragmentContext)
        .doOnSuccess(fr -> logDoAction(actionLogger, startTime, fr));
  }

  private void savePayloadToCache(String cacheKey, FragmentResult fragmentResult,
      ActionLogger actionLogger) {
    if (isCacheable(fragmentResult)) {
      Object resultPayload = getAppendedPayload(fragmentResult);
      logCacheMiss(actionLogger, cacheKey, resultPayload);
      tryToPutInCache(cacheKey, resultPayload, actionLogger);
    } else {
      logCachePass(actionLogger, cacheKey);
    }
  }

  private void tryToPutInCache(String cacheKey, Object resultPayload, ActionLogger actionLogger) {
    try {
      cache.put(cacheKey, resultPayload);
    } catch (Exception e) {
      logCachePutFailure(actionLogger, cacheKey, resultPayload, e);
      if (failWhenCachePutFails) {
        throw e;
      }
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

  private FragmentResult toSuccessTransition(Fragment fragment) {
    return new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
  }

  private FragmentResult toErrorTransition(FragmentContext fragmentContext) {
    return new FragmentResult(fragmentContext.getFragment(), FragmentResult.ERROR_TRANSITION);
  }

  private String notEmptyPayloadKey(CacheOptions options) {
    String key = options.getPayloadKey();
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Action requires payloadKey value in configuration.");
    }
    return key;
  }

  private String notEmptyCacheKey(CacheOptions options) {
    String key = options.getCacheKey();
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Action requires cacheKey value in configuration.");
    }
    return key;
  }

  private String computeCacheKey(FragmentContext fragmentContext) {
    return new FragmentContextPlaceholderResolver(fragmentContext).resolve(cacheKeySchema);
  }

  private static boolean isSuccessTransition(FragmentResult fragmentResult) {
    return FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition());
  }

  private static void logDoAction(ActionLogger actionLogger, long startTime,
      FragmentResult fragmentResult) {
    long executionTime = TimeCalculator.executionTime(startTime);
    if (isSuccessTransition(fragmentResult)) {
      actionLogger.doActionLog(executionTime, fragmentResult.getLog());
    } else {
      actionLogger.failureDoActionLog(executionTime, fragmentResult.getLog());
    }
  }

  private static void logCacheHit(ActionLogger actionLogger, String cacheKey, Object cachedValue) {
    actionLogger.info(CACHE_HIT,
        new JsonObject().put(CACHE_KEY, cacheKey).put(CACHED_VALUE, cachedValue));
  }

  private static void logCacheMiss(ActionLogger actionLogger, String cacheKey,
      Object computedValue) {
    actionLogger.info(CACHE_MISS,
        new JsonObject().put(CACHE_KEY, cacheKey).put(COMPUTED_VALUE, computedValue));
  }

  private static void logCachePass(ActionLogger actionLogger, String cacheKey) {
    actionLogger.error(CACHE_PASS, new JsonObject().put(CACHE_KEY, cacheKey));
  }

  private static void logCacheGetFailure(ActionLogger actionLogger, String cacheKey, Throwable e) {
    actionLogger.error(CACHE_GET_FAILURE, new JsonObject().put(CACHE_KEY, cacheKey));
    actionLogger.error(e);
  }

  private static void logCachePutFailure(ActionLogger actionLogger, String cacheKey,
      Object computedValue, Throwable e) {
    actionLogger.error(CACHE_PUT_FAILURE,
        new JsonObject().put(CACHE_KEY, cacheKey).put(COMPUTED_VALUE, computedValue));
    actionLogger.error(e);
  }
}
