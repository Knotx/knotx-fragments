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

import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.vertx.core.json.JsonObject;

public class CacheActionLogger {

  public static final String CACHE_KEY = "cache_key";
  public static final String CACHED_VALUE = "cached_value";
  public static final String COMPUTED_VALUE = "computed_value";
  public static final String CACHE_MISS = "cache_miss";
  public static final String CACHE_HIT = "cache_hit";
  public static final String CACHE_PASS = "cache_pass";

  private final ActionLogger actionLogger;
  private String key;

  public static CacheActionLogger create(String alias, ActionLogLevel logLevel) {
    return new CacheActionLogger(ActionLogger.create(alias, logLevel));
  }

  CacheActionLogger(ActionLogger actionLogger) {
    this.actionLogger = actionLogger;
  }

  void onLookup(String key) {
    this.key = key;
  }

  public void onInvocationFinish(ActionInvocation invocation) {
    if (isSuccess(invocation)) {
      actionLogger.info(invocation);
    } else {
      actionLogger.error(invocation);
    }
  }

  private boolean isSuccess(ActionInvocation invocation) {
    return invocation.isResultDelivered() && invocation.getFragmentResult().isSuccess();
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

  public void onError(Throwable error) {
    actionLogger.error(error);
  }

  public JsonObject getLogAsJson() {
    return actionLogger.toLog().toJson();
  }

}
