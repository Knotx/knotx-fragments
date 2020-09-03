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

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import java.util.function.Supplier;

public final class TestUtils {

  public static final String CACHE_KEY = "cacheKey";
  public static final String ACTION_ALIAS = "alias";
  public static final String PAYLOAD_KEY = "payloadKey";
  public static final JsonObject SOME_VALUE = new JsonObject().put("configuration", "value");

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

}
