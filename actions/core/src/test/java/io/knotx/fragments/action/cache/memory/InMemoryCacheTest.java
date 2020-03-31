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
package io.knotx.fragments.action.cache.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.action.cache.Cache;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class InMemoryCacheTest {

  private static final JsonObject CONFIG = new JsonObject()
      .put("enableMaximumSize", false)
      .put("enableTtl", false)
      .put("enableTtlAfterAccess", false);

  @Test
  @DisplayName("Should cache 5001 elements when maximum size option disabled")
  void cache5001Elements(VertxTestContext testContext) throws InterruptedException {
    // given
    JsonObject config = CONFIG.copy();

    Cache tested = new InMemoryCache(new InMemoryCacheOptions(config));

    // when
    putIntoCache(tested, 5000);

    // then
    verifyCacheContains(testContext, tested, 5000, 5000)
        .doOnComplete(testContext::completeNow)
        .subscribe();

    testContext.awaitCompletion(5000, TimeUnit.MILLISECONDS);
  }

  @Test
  @DisplayName("Should fit 1000 elements when maximum size set to 1000")
  void cacheMaximumNumberOfElements(VertxTestContext testContext) throws InterruptedException {
    // given
    JsonObject config = CONFIG.copy()
        .put("enableMaximumSize", true)
        .put("maximumSize", 1000);

    Cache tested = new InMemoryCache(new InMemoryCacheOptions(config));

    // when
    putIntoCache(tested, 5000);

    // then
    verifyCacheContains(testContext, tested, 5000, 1000)
        .doOnComplete(testContext::completeNow)
        .subscribe();

    testContext.awaitCompletion(5000, TimeUnit.MILLISECONDS);
  }

  @Test
  @DisplayName("Should not contain stale elements when ttl (after write) set to 300ms")
  void evictWhenTtlAfterWriteSet(VertxTestContext testContext) throws InterruptedException {
    //
    int elementsPut = 200;
    int ttlMs = 300;

    JsonObject config = CONFIG.copy()
        .put("enableTtl", true)
        .put("ttl", ttlMs);

    Cache tested = new InMemoryCache(new InMemoryCacheOptions(config));

    // when
    putIntoCache(tested, elementsPut);

    verifyCacheContains(testContext, tested, elementsPut, elementsPut)
        .subscribe();

    // then
    verifyCacheContains(testContext, tested, elementsPut, 0)
        .doOnComplete(testContext::completeNow)
        .delaySubscription(ttlMs, TimeUnit.MILLISECONDS)
        .subscribe();

    testContext.awaitCompletion(5000, TimeUnit.MILLISECONDS);
  }

  @Test
  @DisplayName("Should not contain stale elements when ttl (after access) set to 500ms")
  void evictWhenTtlAfterAccessSet(VertxTestContext testContext) throws InterruptedException {
    int elementsPut = 50;
    int accessDelay = 100;
    int ttlAfterAccessMs = 500;

    JsonObject config = CONFIG.copy()
        .put("enableTtlAfterAccess", true)
        .put("ttlAfterAccess", ttlAfterAccessMs);

    Cache tested = new InMemoryCache(new InMemoryCacheOptions(config));

    // when
    putIntoCache(tested, elementsPut);

    // then
    verifyCacheContains(testContext, tested, elementsPut, elementsPut)
        .delaySubscription(accessDelay, TimeUnit.MILLISECONDS)
        .doOnComplete(() ->
            verifyCacheContains(testContext, tested, elementsPut, 0)
                .doOnComplete(testContext::completeNow)
                .delaySubscription(ttlAfterAccessMs, TimeUnit.MILLISECONDS)
                .subscribe())
        .subscribe();

    testContext.awaitCompletion(5000, TimeUnit.MILLISECONDS);
  }

  private void putIntoCache(Cache cache, int elements) {
    IntStream.rangeClosed(1, elements).forEach(i -> cache.put(String.valueOf(i), i));
  }

  private Observable verifyCacheContains(VertxTestContext testContext, Cache tested, int testRange,
      int expected) {
    AtomicInteger found = new AtomicInteger(0);
    return Observable.fromIterable(() -> IntStream.rangeClosed(1, testRange).iterator())
        .flatMapMaybe(i -> tested.get(String.valueOf(i))
            .filter(x -> x.equals(i))
            .doOnSuccess(x -> found.incrementAndGet())
        )
        .doOnComplete(() -> testContext.verify(() -> assertEquals(expected, found.get())))
        .doOnError(testContext::failNow)
        .onErrorReturn(e -> new Object());
  }

}
