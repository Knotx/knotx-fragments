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

import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheActionOptionsTest {

  private static final JsonObject CACHE_CONFIG = new JsonObject()
      .put("ttlMs", 2000L)
      .put("maximumSize", 500L);

  @Test
  @DisplayName("Expect successful serialization and deserialization")
  void copyFromJson() {
    CacheActionOptions original = new CacheActionOptions(new JsonObject())
        .setType("in-memory")
        .setCacheKey("cacheKey")
        .setPayloadKey("payloadKey")
        .setLogLevel("error")
        .setCache(CACHE_CONFIG);

    CacheActionOptions copy = new CacheActionOptions(original.toJson());

    assertEquals(original, copy);
    assertJsonEquals(original.toJson(), copy.toJson());
  }

}
