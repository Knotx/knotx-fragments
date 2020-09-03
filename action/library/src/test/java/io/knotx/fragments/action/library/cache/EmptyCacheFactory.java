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

import static io.knotx.fragments.action.library.cache.TestUtils.EMPTY_CACHE;

import io.knotx.commons.cache.Cache;
import io.knotx.commons.cache.CacheFactory;
import io.vertx.core.json.JsonObject;

public class EmptyCacheFactory implements CacheFactory {

  @Override
  public String getType() {
    return "empty-cache";
  }

  @Override
  public Cache create(JsonObject config) {
    if (config.containsKey("invalidOption")) {
      throw new IllegalArgumentException();
    }
    return EMPTY_CACHE.get();
  }
}
