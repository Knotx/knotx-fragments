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

import static io.knotx.fragments.action.library.cache.TestUtils.CACHE_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.PAYLOAD_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.SOME_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.knotx.commons.cache.Cache;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheStoreTest {

  @Mock
  private Cache cache;

  @Mock
  private CacheActionLogger logger;

  @Test
  @DisplayName("Expect appended payload stored in cache and MISS logged")
  void successWithPayload() {
    CacheStore tested = new CacheStore(cache, PAYLOAD_KEY);

    tested.save(logger, CACHE_KEY, successResultWithPayload(SOME_VALUE));

    verify(cache, times(1)).put(CACHE_KEY, SOME_VALUE);
    verify(logger, times(1)).onMiss(SOME_VALUE);
  }

  @Test
  @DisplayName("Expect null payload stored in cache and MISS logged")
  void successWithNull() {
    CacheStore tested = new CacheStore(cache, PAYLOAD_KEY);

    tested.save(logger, CACHE_KEY, successResultWithPayload(null));

    verify(cache, times(1)).put(CACHE_KEY, null);
    verify(logger, times(1)).onMiss(null);
  }

  @Test
  @DisplayName("Expect cache untouched when no payload and PASS logged")
  void successNoPayload() {
    CacheStore tested = new CacheStore(cache, PAYLOAD_KEY);

    tested.save(logger, CACHE_KEY, successResultNoPayload());

    verify(cache, times(0)).put(any(), any());
    verify(logger, times(1)).onPass();
  }

  @Test
  @DisplayName("Expect failed FragmentResult's payload not stored and PASS logged")
  void failureWithPayload() {
    CacheStore tested = new CacheStore(cache, PAYLOAD_KEY);

    tested.save(logger, CACHE_KEY, errorResultWithPayload());

    verify(cache, times(0)).put(any(), any());
    verify(logger, times(1)).onPass();
  }

  private FragmentResult successResultNoPayload() {
    return FragmentResult.success(new Fragment("some-id", new JsonObject(), ""));
  }

  private FragmentResult successResultWithPayload(JsonObject payload) {
    return FragmentResult.success(
        new Fragment("some-id", new JsonObject(), "")
            .appendPayload(PAYLOAD_KEY, payload)
    );
  }

  private FragmentResult errorResultWithPayload() {
    return FragmentResult.fail(
        new Fragment("some-id", new JsonObject(), "")
            .appendPayload(PAYLOAD_KEY, SOME_VALUE),
        new RuntimeException()
    );
  }

}
