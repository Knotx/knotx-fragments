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
package io.knotx.fragments.action.library.cache;

import static io.knotx.commons.validation.ValidationHelper.checkArgument;
import static io.knotx.fragments.action.api.invoker.ActionInvoker.rxApply;
import static io.knotx.fragments.action.library.helper.FragmentPlaceholders.buildSourceDefinitions;

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.api.SingleAction;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.library.cache.operations.CacheActionLogger;
import io.knotx.fragments.action.library.cache.operations.CacheLookup;
import io.knotx.fragments.action.library.cache.operations.CacheStore;
import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.apache.commons.lang3.StringUtils;

public class CacheAction implements SingleAction {

  private final boolean failWhenLookupFails;
  private final boolean failWhenStoreFails;

  private final ActionLogLevel logLevel;
  private final String keySchema;
  private final String alias;

  private final Action doAction;

  private final CacheLookup lookup;
  private final CacheStore store;

  public static CacheAction create(Cache cache, CacheActionOptions options, String alias,
      Action doAction) {
    checkArgument(StringUtils.isBlank(options.getPayloadKey()),
        () -> new ActionConfigurationException(alias,
            "CacheAction requires payloadKey value in configuration."));
    checkArgument(StringUtils.isBlank(options.getCacheKey()),
        () -> new ActionConfigurationException(alias,
            "CacheAction requires cacheKey value in configuration."));
    checkArgument(doAction == null, () -> new ActionConfigurationException(alias,
        "CacheAction requires doAction configured but none provided"));
    return new CacheAction(
        alias,
        options.getCacheKey(),
        doAction,
        ActionLogLevel.fromConfig(options.getLogLevel(), ActionLogLevel.ERROR),
        new CacheLookup(cache, options.getPayloadKey()),
        new CacheStore(cache, options.getPayloadKey()),
        options.isFailWhenLookupFails(),
        options.isFailWhenStoreFails()
    );
  }

  public CacheAction(String alias, String keySchema, Action doAction, ActionLogLevel logLevel,
      CacheLookup lookup, CacheStore store, boolean failWhenLookupFails,
      boolean failWhenStoreFails) {
    this.alias = alias;
    this.doAction = doAction;
    this.keySchema = keySchema;
    this.logLevel = logLevel;
    this.lookup = lookup;
    this.store = store;
    this.failWhenLookupFails = failWhenLookupFails;
    this.failWhenStoreFails = failWhenStoreFails;
  }

  @Override
  public Single<FragmentResult> apply(FragmentContext fragmentContext) {
    CacheActionLogger logger = CacheActionLogger.create(alias, logLevel);
    String cacheKey = createCacheKey(fragmentContext);

    return lookupInCache(cacheKey, fragmentContext, logger)
        .switchIfEmpty(retrieveAndStore(cacheKey, fragmentContext, logger))
        .onErrorReturn(error -> FragmentResult.fail(fragmentContext.getFragment(), error))
        .map(result -> result.copyWithNewLog(logger.getLogAsJson()));
  }

  private Maybe<FragmentResult> lookupInCache(String cacheKey, FragmentContext context,
      CacheActionLogger logger) {
    return lookup.find(cacheKey, logger)
        .map(value -> lookup.toResponse(context, value))
        .onErrorResumeNext(error -> { return handleLookupError(logger, error); });
  }

  private Single<FragmentResult> retrieveAndStore(String cacheKey, FragmentContext context,
      CacheActionLogger logger) {
    return rxApply(doAction, context)
        .doOnSuccess(logger::onInvocationFinish)
        .doOnSuccess(ActionInvocation::rethrowIfResultNotDelivered)
        .doOnSuccess(invocation -> safeSave(logger, cacheKey, invocation))
        .map(ActionInvocation::getFragmentResult);
  }

  private String createCacheKey(FragmentContext context) {
    SourceDefinitions sourceDefinitions = buildSourceDefinitions(context);
    return PlaceholdersResolver.createEncoding(sourceDefinitions).resolve(keySchema);
  }

  private Maybe<FragmentResult> handleLookupError(CacheActionLogger logger, Throwable error) {
    logger.onError(error);
    if (failWhenLookupFails) {
      return Maybe.error(error);
    } else {
      return Maybe.empty();
    }
  }

  private void safeSave(CacheActionLogger logger, String cacheKey, ActionInvocation invocation) {
    try {
      store.save(logger, cacheKey, invocation.getFragmentResult());
    } catch (Throwable e) {
      logger.onError(e);
      if (failWhenStoreFails) {
        throw e;
      }
    }
  }

}
