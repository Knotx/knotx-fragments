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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

@ExtendWith(VertxExtension.class)
class CacheActionTest {

  private static final String ACTION_ALIAS = "action";
  private static final String PAYLOAD_KEY = "product";
  private static final String CACHE_KEY = "product-{param.id}";
  private static final String COMPLEX_CACHE_KEY = "product-{param.id}-{config.isOpen}-{payload.call-service._result.code}";
  private static final String INTERPOLATED_CACHE_KEY = "product-khj24-true-200";

  private static final String PARAM_SAMPLE = "khj24";
  private static final String CONFIG_SAMPLE = "true";
  private static final String PAYLOAD_SAMPLE = "200";

  private static final JsonObject SAMPLE_CACHEABLE_PAYLOAD = new JsonObject()
      .put("key", "value");

  private static final JsonObject CONFIG = new JsonObject()
      .put("cacheKey", CACHE_KEY)
      .put("payloadKey", PAYLOAD_KEY)
      .put("logLevel", "info");

  private Fragment sampleFragment;

  @BeforeEach
  void setUp() {
    sampleFragment = new Fragment("type", new JsonObject(), "initial body");
  }

  @Test
  @DisplayName("Expect exception when payload key is not specified")
  void noPayloadKey() {
    // given
    JsonObject config = CONFIG.copy();
    config.remove("payloadKey");
    CacheOptions options = new CacheOptions(config);

    // when, then
    assertThrows(IllegalArgumentException.class,
        () -> new CacheAction(ACTION_ALIAS, noPayloadSuccessfulDoAction(), options, emptyCache()));
  }

  @Test
  @DisplayName("Expect exception when cache key schema is not specified")
  void noCacheKey() {
    // given
    JsonObject config = CONFIG.copy();
    config.remove("cacheKey");
    CacheOptions options = new CacheOptions(config);

    // when, then
    assertThrows(IllegalArgumentException.class,
        () -> new CacheAction(ACTION_ALIAS, noPayloadSuccessfulDoAction(), options, emptyCache()));
  }

  @Test
  @DisplayName("Expect exception when invalid log level set")
  void invalidLogLevel() {
    // given
    CacheOptions options = new CacheOptions(CONFIG.copy().put("logLevel", "invalid"));

    // when, then
    assertThrows(IllegalArgumentException.class,
        () -> new CacheAction(ACTION_ALIAS, noPayloadSuccessfulDoAction(), options, emptyCache()));
  }

  @Test
  @DisplayName("Expect a single cache lookup")
  void lookupInCacheOnce(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    CacheOptions options = new CacheOptions(CONFIG.copy());
    Cache cache = cacheReturning(anyString -> Maybe.just("Found!"));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(cache, times(1)).get(any());
    });
  }

  @Test
  @DisplayName("Expect cache lookup with interpolated cache key")
  void lookupInCacheWithInterpolatedValue(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    CacheOptions options = new CacheOptions(CONFIG.copy().put("cacheKey", COMPLEX_CACHE_KEY));
    Cache cache = cacheReturning(anyString -> Maybe.just("Found!"));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    FragmentContext input = sampleInputWihtParamConfigAndPayload();
    // when
    verifyExecution(testContext, tested, input, result -> {
      // then
      assertTrue(result.succeeded());
      verify(cache, times(1)).get(INTERPOLATED_CACHE_KEY);
    });
  }

  @Test
  @DisplayName("Expect doAction not called when value found in cache")
  void lookupInCacheAndDontCallDoAction(VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    CacheOptions options = new CacheOptions(CONFIG.copy());
    Cache cache = cacheReturning(anyString -> Maybe.just("Found!"));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(doAction, times(0)).apply(any(), any());
    });
  }

  @Test
  @DisplayName("Expect cached value populated when found in cache")
  void lookupInCacheAndReturnCachedValue(VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    CacheOptions options = new CacheOptions(CONFIG.copy());
    Cache cache = cacheReturning(anyString -> Maybe.just(SAMPLE_CACHEABLE_PAYLOAD));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      JsonObject actual = result.result().getFragment().getPayload().getJsonObject(PAYLOAD_KEY);
      assertEquals(SAMPLE_CACHEABLE_PAYLOAD, actual);
    });
  }

  @Test
  @DisplayName("Expect doAction called when value not found in cache")
  void lookupInCacheAndCallDoAction(VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    CacheOptions options = new CacheOptions(CONFIG.copy());
    Cache cache = emptyCache();

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(doAction, times(1)).apply(any(), any());
    });
  }

  @Test
  @DisplayName("Expect cache not populated when no cacheable data returned by doAction")
  void callDoActionThatHasNoRelevantData(VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = emptyCache();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(cache, times(0)).put(any(), any());
    });
  }

  @Test
  @DisplayName("Expect cache populated when doAction has _success transition and payload")
  void callDoActionAndCache(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = successfulDoActionWithPayload();
    Cache cache = emptyCache();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(cache, times(1)).put("product-", SAMPLE_CACHEABLE_PAYLOAD);
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"_custom", "_error"})
  @DisplayName("Expect cache not populated when transition is not successful")
  void callDoActionWithUnsuccessfulTransition(String transition, VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = doActionWithPayload(transition);
    Cache cache = emptyCache();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(cache, times(0)).put(any(), any());
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"_success", "_custom", "_error"})
  @DisplayName("Expect the same transition as doAction when called")
  void callDoActionAndPassOnTransition(String transition, VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = noPayloadDoAction(transition);
    Cache cache = emptyCache();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(doAction, times(1)).apply(any(), any());
      assertEquals(transition, result.result().getTransition());
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"_success", "_custom", "_error"})
  @DisplayName("Expect doAction's payload is passed for any transition")
  void callDoActionAndPassPayload(String transition, VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = doActionWithPayload(transition);
    Cache cache = emptyCache();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(SAMPLE_CACHEABLE_PAYLOAD,
          result.result().getFragment().getPayload().getJsonObject(PAYLOAD_KEY));
    });
  }

  @Test
  @DisplayName("Expect _error transition when doAction fails")
  void callFailingDoAction(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = failingDoAction();
    Cache cache = emptyCache();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(FragmentResult.ERROR_TRANSITION, result.result().getTransition());
    });
  }

  @Test
  @DisplayName("Expect _error transition when getting from cache results in a failed Maybe as a default behaviour")
  void callGetOnFailingCache(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = cacheFailingOnGet();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(FragmentResult.ERROR_TRANSITION, result.result().getTransition());
    });
  }

  @Test
  @DisplayName("Expect doAction to be called when getting from cache results in a failed Maybe and custom option configured")
  void callGetOnFailingCacheConfigured(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = cacheFailingOnGet();
    CacheOptions options = new CacheOptions(CONFIG.copy().put("failWhenCacheGetFails", false));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(doAction, times(1)).apply(any(), any());
    });
  }

  @Test
  @DisplayName("Expect _error transition when getting from cache returns null as a default behaviour")
  void callGetOnNullingCache(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = cacheReturningNull();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(FragmentResult.ERROR_TRANSITION, result.result().getTransition());
    });
  }

  @Test
  @DisplayName("Expect doAction to be called when getting from cache returns null and custom option configured")
  void callGetOnNullingCacheConfigured(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = cacheReturningNull();
    CacheOptions options = new CacheOptions(CONFIG.copy().put("failWhenCacheGetFails", false));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(doAction, times(1)).apply(any(), any());
    });
  }

  @Test
  @DisplayName("Expect _error transition when getting from cache throws an exception as a default behaviour")
  void callGetOnThrowingCache(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = cacheThrowingOnGet();
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    //when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(FragmentResult.ERROR_TRANSITION, result.result().getTransition());
    });
  }

  @Test
  @DisplayName("Expect doAction to be called when getting from cache throws an exception and custom option configured")
  void callGetOnThrowingCacheConfigured(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = noPayloadSuccessfulDoAction();
    Cache cache = cacheThrowingOnGet();
    CacheOptions options = new CacheOptions(CONFIG.copy().put("failWhenCacheGetFails", false));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      verify(doAction, times(1)).apply(any(), any());
    });
  }

  @Test
  @DisplayName("Expect normal processing when putting to cache results in an exception as a default behaviour")
  void callPutOnFailingCache(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = successfulDoActionWithPayload();
    Cache cache = cacheThrowingOnPut(key -> Maybe.empty());
    CacheOptions options = new CacheOptions(CONFIG.copy());

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(SAMPLE_CACHEABLE_PAYLOAD,
          result.result().getFragment().getPayload().getJsonObject(PAYLOAD_KEY));
    });
  }

  @Test
  @DisplayName("Expect _error transition when putting to cache results in an exception and option configured")
  void callPutOnFailingCacheConfigured(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = successfulDoActionWithPayload();
    Cache cache = cacheThrowingOnPut(key -> Maybe.empty());
    CacheOptions options = new CacheOptions(CONFIG.copy().put("failWhenCachePutFails", true));

    Action tested = new CacheAction(ACTION_ALIAS, doAction, options, cache);

    // when
    verifyExecution(testContext, tested, result -> {
      // then
      assertTrue(result.succeeded());
      assertEquals(FragmentResult.ERROR_TRANSITION, result.result().getTransition());
    });
  }

  private void verifyExecution(VertxTestContext testContext, Action tested,
      Consumer<AsyncResult<FragmentResult>> assertions) throws Throwable {
    verifyExecution(testContext, tested, sampleInput(), assertions);
  }

  private void verifyExecution(VertxTestContext testContext, Action tested, FragmentContext input,
      Consumer<AsyncResult<FragmentResult>> assertions) throws Throwable {
    tested.apply(input, result -> {
      testContext.verify(() -> assertions.accept(result));
      testContext.completeNow();
    });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private Action successfulDoActionWithPayload() {
    return doActionWithPayload(FragmentResult.SUCCESS_TRANSITION);
  }

  private Action doActionWithPayload(String transition) {
    return doAction(sampleFragment.appendPayload(PAYLOAD_KEY,
        CacheActionTest.SAMPLE_CACHEABLE_PAYLOAD), transition);
  }

  private Action noPayloadSuccessfulDoAction() {
    return noPayloadDoAction(FragmentResult.SUCCESS_TRANSITION);
  }

  private Action failingDoAction() {
    return (fragmentContext, resultHandler) -> Future
        .<FragmentResult>failedFuture("Internal")
        .setHandler(resultHandler);
  }

  private Action noPayloadDoAction(String transition) {
    return doAction(sampleFragment, transition);
  }

  private Action doAction(Fragment fragment, String transition) {
    Action doAction = Mockito.mock(Action.class);
    doAnswer(invocation -> Future
        .succeededFuture(new FragmentResult(fragment, transition))
        .setHandler(invocation.getArgument(1))).when(doAction).apply(any(), any());
    return doAction;
  }

  private Cache cacheReturningNull() {
    return cacheReturning(key -> null);
  }

  private Cache cacheFailingOnGet() {
    return cacheReturning(key -> Maybe.error(new RuntimeException()));
  }

  private Cache cacheThrowingOnGet() {
    Cache cache = Mockito.mock(Cache.class);
    doThrow(RuntimeException.class).when(cache).get(any());
    return cache;
  }

  private Cache cacheThrowingOnPut(Function<String, Maybe<Object>> getter) {
    Cache cache = cacheReturning(getter);
    doThrow(RuntimeException.class).when(cache).put(any(), any());
    return cache;
  }

  private Cache emptyCache() {
    return cacheReturning(key -> Maybe.empty());
  }

  private Cache cacheReturning(Function<String, Maybe<Object>> getter) {
    Cache cache = Mockito.mock(Cache.class);
    doAnswer(invocation -> getter.apply(invocation.getArgument(0))).when(cache).get(anyString());
    return cache;
  }

  private FragmentContext sampleInput() {
    return new FragmentContext(sampleFragment, new ClientRequest());
  }

  private FragmentContext sampleInputWihtParamConfigAndPayload() {
    return new FragmentContext(
        new Fragment("snippet",
            new JsonObject().put("isOpen", CONFIG_SAMPLE),
            "")
            .appendPayload("call-service",
                new JsonObject()
                    .put("_result", new JsonObject()
                        .put("code", PAYLOAD_SAMPLE))),
        new ClientRequest()
            .setParams(
                MultiMap.caseInsensitiveMultiMap()
                    .add("id", PARAM_SAMPLE)
            )
    );
  }
}
