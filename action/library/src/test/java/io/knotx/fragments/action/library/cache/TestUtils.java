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

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.function.Supplier;

public final class TestUtils {

  public static final String CACHE_KEY = "cacheKey";
  public static final String ACTION_ALIAS = "alias";
  public static final String PAYLOAD_KEY = "payloadKey";
  public static final JsonObject SOME_VALUE = new JsonObject().put("configuration", "value");

  public static final String INVOCATIONS_LOGS_KEY = "doActionLogs";

  public static final JsonObject DO_ACTION_LOGS = new JsonObject()
      .put("alias", "some-do-action")
      .put("logs", new JsonObject()
          .put("InnerInfo", "InnerValue"))
      .put("doActionLogs", new JsonArray());

  private TestUtils() {
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

  public static FragmentContext someFragmentContext() {
    return new FragmentContext(new Fragment("type", new JsonObject(), "initial body"),
        new ClientRequest());
  }

  public static Action doActionFatal(Supplier<RuntimeException> generator) {
    return (fragmentContext, resultHandler) -> Future
        .<FragmentResult>failedFuture(generator.get())
        .onComplete(resultHandler);
  }

  public static Action doActionIdle() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      Future
          .succeededFuture(new FragmentResult(fragment, SUCCESS_TRANSITION, DO_ACTION_LOGS))
          .onComplete(resultHandler);
    };
  }

  public static Action doActionError() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      Future
          .succeededFuture(new FragmentResult(fragment, ERROR_TRANSITION, DO_ACTION_LOGS))
          .onComplete(resultHandler);
    };
  }

  public static Action doActionAppending() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(PAYLOAD_KEY, SOME_VALUE);
      Future
          .succeededFuture(new FragmentResult(fragment, SUCCESS_TRANSITION))
          .onComplete(resultHandler);
    };
  }

  public static Action doActionReturning(FragmentResult fragmentResult) {
    return (fragmentContext, resultHandler) -> Future
        .succeededFuture(fragmentResult)
        .onComplete(resultHandler);
  }

}
