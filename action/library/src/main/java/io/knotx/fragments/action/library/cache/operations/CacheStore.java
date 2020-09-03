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
package io.knotx.fragments.action.library.cache.operations;

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.api.FragmentResult;

public class CacheStore {

  private final Cache cache;
  private final String payloadKey;

  public CacheStore(Cache cache, String payloadKey) {
    this.cache = cache;
    this.payloadKey = payloadKey;
  }

  public void save(CacheActionLogger logger, String cacheKey, FragmentResult fragmentResult) {
    // TODO: configurable behaviour in case of error in storage?
    // TODO: should store null?
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

  private static boolean isSuccessTransition(FragmentResult fragmentResult) {
    return FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition());
  }

}
