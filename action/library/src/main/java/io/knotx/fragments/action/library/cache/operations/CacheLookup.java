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

import static io.knotx.fragments.api.FragmentResult.success;

import io.knotx.fragments.action.library.cache.Cache;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.reactivex.Maybe;

public class CacheLookup {

  private final Cache cache;
  private final String payloadKey;

  public CacheLookup(Cache cache, String payloadKey) {
    this.cache = cache;
    this.payloadKey = payloadKey;
  }

  public Maybe<Object> find(String cacheKey, CacheActionLogger logger) {
    return Maybe.just(cacheKey)
        .doOnSuccess(logger::onLookup)
        .flatMap(cache::get)
        .doOnSuccess(logger::onHit);
  }

  public FragmentResult toResponse(FragmentContext original, Object cachedValue) {
    return success(putInPayload(original, cachedValue));
  }

  private Fragment putInPayload(FragmentContext original, Object cachedValue) {
    return original.getFragment().appendPayload(payloadKey, cachedValue);
  }

}
