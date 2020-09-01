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

import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHED_VALUE;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_HIT;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_KEY;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_MISS;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_PASS;
import static io.knotx.fragments.action.library.InMemoryCacheAction.COMPUTED_VALUE;
import static java.time.Instant.now;

import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.action.library.helper.TimeCalculator;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;

class CacheActionLogger {

  private final ActionLogger actionLogger;
  private String key;
  private long retrieveStart;

  public CacheActionLogger(ActionLogger actionLogger) {
    this.actionLogger = actionLogger;
  }

  void onLookup(String key) {
    this.key = key;
  }

  void onRetrieveStart() {
    this.retrieveStart = now().toEpochMilli();
  }

  void onRetrieveEnd(FragmentResult fragmentResult) {
    long executionTime = TimeCalculator.executionTime(retrieveStart);
    if (isSuccessTransition(fragmentResult)) {
      actionLogger.doActionLog(executionTime, fragmentResult.getLog());
    } else {
      actionLogger.failureDoActionLog(executionTime, fragmentResult.getLog());
    }
  }

  void onHit(Object cachedValue) {
    actionLogger.info(CACHE_HIT, new JsonObject()
        .put(CACHE_KEY, key)
        .put(CACHED_VALUE, cachedValue));
  }

  void onMiss(Object computedValue) {
    actionLogger.info(CACHE_MISS, new JsonObject()
        .put(CACHE_KEY, key)
        .put(COMPUTED_VALUE, computedValue));
  }

  void onPass() {
    actionLogger.error(CACHE_PASS, new JsonObject()
        .put(CACHE_KEY, key));
  }

  public JsonObject getLogAsJson() {
    return actionLogger.toLog().toJson();
  }

  private static boolean isSuccessTransition(FragmentResult fragmentResult) {
    return FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition());
  }
}
