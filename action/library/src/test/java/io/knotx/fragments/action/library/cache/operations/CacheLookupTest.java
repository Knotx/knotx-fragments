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

import static io.knotx.fragments.action.library.cache.TestUtils.EMPTY_CACHE;
import static io.knotx.fragments.action.library.cache.TestUtils.ERROR_CACHE;
import static io.knotx.fragments.action.library.cache.TestUtils.PAYLOAD_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.SAMPLE_CACHE;
import static io.knotx.fragments.action.library.cache.TestUtils.SOME_VALUE;
import static io.knotx.fragments.action.library.cache.TestUtils.THROWING_CACHE;
import static io.knotx.fragments.action.library.cache.TestUtils.someFragmentContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.knotx.fragments.api.FragmentResult;
import io.knotx.junit5.KnotxExtension;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(KnotxExtension.class)
class CacheLookupTest {

  @Mock
  private CacheActionLogger logger;

  @Test
  @DisplayName("Expect cache hit logged on INFO level")
  void cacheHit(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(SAMPLE_CACHE.get(), PAYLOAD_KEY);

    tested.find("someKey", logger)
        .subscribe(value -> testContext.verify(() -> {
              verify(logger, times(1)).onHit(SOME_VALUE);
              testContext.completeNow();
            }),
            testContext::failNow,
            () -> testContext.failNow(new RuntimeException("Expected success")));
  }

  @Test
  @DisplayName("Expect successful Maybe returned when value present in cache")
  void valuePresent(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(SAMPLE_CACHE.get(), PAYLOAD_KEY);

    tested.find("someKey", logger)
        .subscribe(value -> testContext.verify(() -> {
              assertEquals(SOME_VALUE, value);
              testContext.completeNow();
            }),
            testContext::failNow,
            () -> testContext.failNow(new RuntimeException("Expected success")));
  }

  @Test
  @DisplayName("Expect completed Maybe returned when no value in cache")
  void cacheEmpty(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(EMPTY_CACHE.get(), PAYLOAD_KEY);

    tested.find("someKey", logger)
        .subscribe(value -> testContext.failNow(new RuntimeException()),
            testContext::failNow,
            testContext::completeNow);
  }

  @Test
  @DisplayName("Expect erroneous Maybe returned when error returned from cache")
  void cacheError(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(ERROR_CACHE.get(), PAYLOAD_KEY);

    tested.find("someKey", logger)
        .subscribe(value -> testContext.failNow(new RuntimeException("Expected error")),
            error -> testContext.completeNow(),
            () -> testContext.failNow(new RuntimeException("Expected error")));
  }

  @Test
  @DisplayName("Expect erroneous Maybe returned when cache throws")
  void cacheThrows(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(THROWING_CACHE.get(), PAYLOAD_KEY);

    tested.find("someKey", logger)
        .subscribe(value -> testContext.failNow(new RuntimeException("Expected error")),
            error -> testContext.completeNow(),
            () -> testContext.failNow(new RuntimeException("Expected error")));
  }

  @Test
  @DisplayName("Expect value put in Fragment's payload")
  void someValue() {
    CacheLookup tested = new CacheLookup(EMPTY_CACHE.get(), PAYLOAD_KEY);

    JsonObject expected = new JsonObject().put(PAYLOAD_KEY, SOME_VALUE);
    FragmentResult result = tested.toResponse(someFragmentContext(), SOME_VALUE);

    assertEquals(expected, result.getFragment().getPayload());
  }

}