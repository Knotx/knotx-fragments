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

import static io.knotx.fragments.action.library.TestUtils.someContext;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.EMPTY_CACHE;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.ERROR_CACHE;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.PAYLOAD_KEY;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.SAMPLE_CACHE;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.SOME_VALUE;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.THROWING_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class CacheLookupTest {

  @Mock
  private CacheActionLogger logger;

  @Test
  @DisplayName("Expect cache hit logged")
  void cacheHit(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(SAMPLE_CACHE.get(), PAYLOAD_KEY);

    expectSuccess(testContext, tested, value -> verify(logger, times(1)).onHit(SOME_VALUE));
  }

  @Test
  @DisplayName("Expect successful Maybe returned when value present in cache")
  void valuePresent(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(SAMPLE_CACHE.get(), PAYLOAD_KEY);

    expectSuccess(testContext, tested, value -> assertEquals(SOME_VALUE, value));
  }

  @Test
  @DisplayName("Expect completed Maybe returned when no value in cache")
  void cacheEmpty(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(EMPTY_CACHE.get(), PAYLOAD_KEY);

    expectEmpty(testContext, tested);
  }

  @Test
  @DisplayName("Expect erroneous Maybe returned when error returned from cache")
  void cacheError(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(ERROR_CACHE.get(), PAYLOAD_KEY);

    expectError(testContext, tested);
  }

  @Test
  @DisplayName("Expect erroneous Maybe returned when cache throws")
  void cacheThrows(VertxTestContext testContext) {
    CacheLookup tested = new CacheLookup(THROWING_CACHE.get(), PAYLOAD_KEY);

    expectError(testContext, tested);
  }

  @Test
  @DisplayName("Expect value put in Fragment's payload")
  void someValue() {
    CacheLookup tested = new CacheLookup(EMPTY_CACHE.get(), PAYLOAD_KEY);

    JsonObject expected = new JsonObject().put(PAYLOAD_KEY, SOME_VALUE);
    FragmentResult result = tested.toResponse(someContext(), SOME_VALUE);

    assertEquals(expected, result.getFragment().getPayload());
  }

  private void expectSuccess(VertxTestContext testContext, CacheLookup tested,
      Consumer<Object> assertions) {
    tested.find("some-key", logger)
        .subscribe(value -> testContext.verify(() -> {
              assertions.accept(value);
              testContext.completeNow();
            }),
            testContext::failNow,
            () -> testContext.failNow(new RuntimeException("Expected success")));
  }

  private void expectEmpty(VertxTestContext testContext, CacheLookup tested) {
    tested.find("some-key", logger)
        .subscribe(value -> testContext.failNow(new RuntimeException("Expected error")),
            testContext::failNow,
            testContext::completeNow);
  }

  private void expectError(VertxTestContext testContext, CacheLookup tested) {
    tested.find("some-key", logger)
        .subscribe(value -> testContext.failNow(new RuntimeException("Expected error")),
            error -> testContext.completeNow(),
            () -> testContext.failNow(new RuntimeException("Expected error")));
  }

}
