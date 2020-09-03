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

import static io.knotx.fragments.action.library.helper.FragmentPlaceholders.buildSourceDefinitions;
import static io.knotx.fragments.action.library.helper.ValidationHelper.checkArgument;
import static io.knotx.fragments.api.FragmentResult.fail;

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.SingleAction;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.library.cache.operations.CacheActionLogger;
import io.knotx.fragments.action.library.cache.operations.CacheLookup;
import io.knotx.fragments.action.library.cache.operations.CacheStore;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.reactivex.Single;
import org.apache.commons.lang3.StringUtils;

public class CacheAction implements SingleAction {

  private final ActionLogLevel logLevel;
  private final String keySchema;
  private final String alias;

  private final Action doAction;

  private final CacheLookup lookup;
  private final CacheStore store;

  public CacheAction(Cache cache, CacheActionOptions options, String alias, Action doAction) {
    checkArgument(alias, StringUtils.isBlank(options.getPayloadKey()),
        "Action requires payloadKey value in configuration.");
    checkArgument(alias, StringUtils.isBlank(options.getCacheKey()),
        "Action requires cacheKey value in configuration.");
    // TODO: validate other arguments?
    this.alias = alias;
    this.doAction = doAction;
    this.keySchema = options.getCacheKey();
    this.logLevel = ActionLogLevel.fromConfig(options.getLogLevel(), ActionLogLevel.ERROR);
    this.lookup = new CacheLookup(cache, options.getPayloadKey());
    this.store = new CacheStore(cache, options.getPayloadKey());
  }

  @Override
  public Single<FragmentResult> apply(FragmentContext fragmentContext) {
    CacheActionLogger logger = CacheActionLogger.create(alias, logLevel);
    String cacheKey = createCacheKey(fragmentContext);

    return lookup.find(cacheKey, logger)
        .map(value -> lookup.toResponse(fragmentContext, value))
        .switchIfEmpty(retrieve(fragmentContext, logger)
            .doOnSuccess(result -> store.save(logger, cacheKey, result)))
        .doOnError(logger::onError)
        .onErrorReturn(error -> fail(fragmentContext.getFragment(), error))
        .map(result -> result.copyWithNewLog(logger.getLogAsJson()));
  }

  private Single<FragmentResult> retrieve(FragmentContext context, CacheActionLogger logger) {
    return Single.just(doAction)
        .map(FragmentOperation::newInstance)
        .doOnSuccess(action -> logger.onRetrieveStart())
        .flatMap(action -> action.rxApply(context))
        .doOnSuccess(logger::onRetrieveEnd);
  }

  private String createCacheKey(FragmentContext context) {
    return PlaceholdersResolver.resolveAndEncode(keySchema, buildSourceDefinitions(context));
  }

}
