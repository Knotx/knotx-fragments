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

import static io.knotx.fragments.action.library.TestUtils.someFragment;

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.api.FragmentResult;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.function.Supplier;

public final class CacheTestUtils {

  public static final String CACHE_KEY = "cacheKey";
  public static final String LOGS_KEY = "logs";
  public static final String ACTION_ALIAS = "alias";
  public static final String PAYLOAD_KEY = "payloadKey";
  public static final JsonObject SOME_VALUE = new JsonObject().put("configuration", "value");

  public static final String INVOCATIONS_LOGS_KEY = "doActionLogs";

  public static final JsonObject DO_ACTION_LOGS = new JsonObject()
      .put("alias", "some-do-action")
      .put("logs", new JsonObject()
          .put("InnerInfo", "InnerValue"))
      .put("doActionLogs", new JsonArray());

  private CacheTestUtils() {
    // Utility class
  }

  public static final Supplier<Cache> EMPTY_CACHE = () -> new Cache() {
    @Override
    public Maybe<Object> get(String key) {
      return Maybe.empty();
    }

    @Override
    public void put(String key, Object value) {

    }
  };

  public static final Supplier<Cache> ERROR_CACHE = () -> new Cache() {
    @Override
    public Maybe<Object> get(String key) {
      return Maybe.error(new IllegalStateException());
    }

    @Override
    public void put(String key, Object value) {

    }
  };

  public static final Supplier<Cache> THROWING_CACHE = () -> new Cache() {
    @Override
    public Maybe<Object> get(String key) {
      throw new IllegalStateException();
    }

    @Override
    public void put(String key, Object value) {
      throw new IllegalStateException();
    }
  };

  public static final Supplier<Cache> SAMPLE_CACHE = () -> new Cache() {
    @Override
    public Maybe<Object> get(String key) {
      return Maybe.just(SOME_VALUE.copy());
    }

    @Override
    public void put(String key, Object value) {
      throw new IllegalStateException();
    }
  };

  public static Action doActionFailed(Supplier<RuntimeException> generator) {
    return (SyncAction) fragmentContext -> {
      throw generator.get();
    };
  }

  public static Action doActionIdleWithLogs() {
    return (SyncAction) fragmentContext -> FragmentResult
        .success(fragmentContext.getFragment(), DO_ACTION_LOGS);
  }

  public static Action doActionError() {
    return (SyncAction) fragmentContext -> FragmentResult
        .fail(fragmentContext.getFragment(), DO_ACTION_LOGS, new RuntimeException());
  }

  public static Action doActionAppending() {
    return (SyncAction) fragmentContext -> FragmentResult
        .success(fragmentContext.getFragment().appendPayload(PAYLOAD_KEY, SOME_VALUE));
  }

  public static Action doActionReturning(FragmentResult fragmentResult) {
    return (SyncAction) fragmentContext -> fragmentResult;
  }

  public static FragmentResult successResultWithPayload(Object payload) {
    return FragmentResult.success(someFragment().appendPayload(PAYLOAD_KEY, payload));
  }

  public static FragmentResult errorResultWithPayload(Object payload) {
    return FragmentResult
        .fail(someFragment().appendPayload(PAYLOAD_KEY, payload), new RuntimeException());
  }

}
