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

import static io.knotx.fragments.action.api.log.ActionInvocationLog.LOG;
import static io.knotx.fragments.action.api.log.ActionInvocationLog.SUCCESS;
import static io.knotx.fragments.action.api.log.ActionLog.INVOCATIONS;
import static io.knotx.fragments.action.library.TestUtils.successResult;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.ACTION_LOG;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.CACHE_KEY;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.LOGS_KEY;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.PAYLOAD_KEY;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.SOME_VALUE;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionAppending;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionFailed;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionIdleWithLogs;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionReturning;
import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.library.cache.operations.CacheLookup;
import io.knotx.fragments.action.library.cache.operations.CacheStore;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class CacheActionTest {

  private static final String KEY_WITH_PLACEHOLDERS = "{header.x}-{param.y}-{payload.z}-{config.p}";
  private static final String KEY_INTERPOLATED = "header-param-payload-config";

  @Mock
  private CacheLookup lookup;

  @Mock
  private CacheStore store;

  @Test
  @DisplayName("Expect cache key is interpolated")
  void cacheKeyInterpolated(VertxTestContext testContext) {
    lookupSucceeding();
    Action tested = create(KEY_WITH_PLACEHOLDERS, doActionIdleWithLogs());

    verifyActionResult(testContext, tested, richContext(),
        result -> verify(lookup, times(1)).find(eq(KEY_INTERPOLATED), any()));
  }

  @Test
  @DisplayName("Expect value returned from lookup is populated")
  void valueFromCachePopulated(VertxTestContext testContext) {
    lookupSucceeding();
    Action tested = create(doActionIdleWithLogs());

    verifyActionResult(testContext, tested,
        result -> assertEquals(SOME_VALUE, result.result().getFragment().getPayload()
            .getJsonObject(PAYLOAD_KEY)));
  }

  @Test
  @DisplayName("Expect doAction not called when value in lookup")
  void doActionNotCalledWhenValueInLookup(VertxTestContext testContext) {
    lookupSucceeding();
    Action doAction = mockedDoAction();
    Action tested = create(doAction);

    verifyActionResult(testContext, tested,
        result -> verify(doAction, times(0)).apply(any(), any()));
  }

  @Test
  @DisplayName("Expect cache store not called when value in lookup")
  void valueNotStoredWhenValueInLookup(VertxTestContext testContext) {
    lookupSucceeding();
    Action tested = create(doActionIdleWithLogs());

    verifyActionResult(testContext, tested,
        result -> verify(store, times(0)).save(any(), any(), any()));
  }

  @Test
  @DisplayName("Expect doAction is called when no value in lookup")
  void doActionCalledWhenNoValueInLookup(VertxTestContext testContext) {
    lookupEmpty();
    Action doAction = mockedDoAction();
    Action tested = create(doAction);

    verifyActionResult(testContext, tested,
        result -> verify(doAction, times(1)).apply(any(), any()));
  }

  @Test
  @DisplayName("Expect doAction call is logged")
  void doActionCallLogged(VertxTestContext testContext) {
    lookupEmpty();
    Action tested = create(doActionIdleWithLogs());

    JsonObject expected = new JsonObject()
        .put(INVOCATIONS, new JsonArray()
            .add(new JsonObject()
                .put(SUCCESS, true)
                .put(LOG, ACTION_LOG)));

    verifyActionResult(testContext, tested,
        result -> assertJsonEquals(expected, result.result().getLog()));
  }

  @Test
  @DisplayName("Expect doAction's result is passed to be stored")
  void doActionResultStored(VertxTestContext testContext) {
    lookupEmpty();
    FragmentResult returned = successResult();
    Action tested = create(doActionReturning(returned));

    verifyActionResult(testContext, tested,
        result -> verify(store, times(1)).save(any(), eq(CACHE_KEY), eq(returned)));
  }

  @Test
  @DisplayName("Expect doAction's result fragment and transition are returned")
  void doActionResultPassed(VertxTestContext testContext) {
    lookupEmpty();
    FragmentResult returned = successResult();
    Action tested = create(doActionReturning(returned));

    verifyActionResult(testContext, tested,
        result -> {
          assertEquals(returned.getFragment(), result.result().getFragment());
          assertEquals(returned.getTransition(), result.result().getTransition());
        });
  }

  @Test
  @DisplayName("Expect lookup failure to prevent from calling doAction")
  void lookupFailurePreventsCallingDoAction(VertxTestContext testContext) {
    lookupFailing();
    Action doAction = mockedDoAction();
    Action tested = create(doAction);

    verifyActionResult(testContext, tested,
        result -> verify(doAction, times(0)).apply(any(), any()));
  }

  @Test
  @DisplayName("Expect throwing doAction prevents from storing information")
  void doActionFailurePreventsStoring(VertxTestContext testContext) {
    lookupEmpty();
    Action tested = create(doActionFailed(RuntimeException::new));

    verifyActionResult(testContext, tested,
        result -> verify(store, times(0)).save(any(), any(), any()));
  }

  @Test
  @DisplayName("Expect lookup failure to result in error transition with original Fragment")
  void lookupFailureEndsInErrorTransition(VertxTestContext testContext) {
    lookupFailing();
    FragmentContext original = richContext();
    FragmentContext copy = new FragmentContext(original.toJson());
    Action tested = create(doActionIdleWithLogs());

    verifyActionResult(testContext, tested, copy,
        result -> {
          assertEquals(original.getFragment(), result.result().getFragment());
          assertEquals(ERROR_TRANSITION, result.result().getTransition());
        });
  }

  @Test
  @DisplayName("Expect throwing doAction to result in error transition with original Fragment")
  void doActionFailureEndsInErrorTransition(VertxTestContext testContext) {
    lookupEmpty();
    FragmentContext original = richContext();
    FragmentContext copy = new FragmentContext(original.toJson());
    Action tested = create(doActionFailed(RuntimeException::new));

    verifyActionResult(testContext, tested, copy,
        result -> {
          assertEquals(original.getFragment(), result.result().getFragment());
          assertEquals(ERROR_TRANSITION, result.result().getTransition());
        });
  }

  @Test
  @DisplayName("Expect exception in storage to result in error transition with original Fragment")
  void saveFailureEndsInErrorTransition(VertxTestContext testContext) {
    lookupEmpty();
    storeFailing();
    FragmentContext original = richContext();
    FragmentContext copy = new FragmentContext(original.toJson());
    Action tested = create(doActionAppending());

    verifyActionResult(testContext, tested, copy,
        result -> {
          assertEquals(original.getFragment(), result.result().getFragment());
          assertEquals(ERROR_TRANSITION, result.result().getTransition());
        });
  }

  @Test
  @DisplayName("Expect lookup failure to be ignored and logged when flag configured")
  void lookupFailureIgnoredWhenConfigured(VertxTestContext testContext) {
    lookupFailing();
    FragmentResult doActionResult = successResult();
    Action tested = create(CACHE_KEY, doActionReturning(doActionResult), false, true);

    JsonObject expectedLog = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put("errors", new JsonArray()
                .add(new JsonObject().put("className", RuntimeException.class.getName()))));

    verifyActionResult(testContext, tested,
        result -> {
          assertEquals(doActionResult.getFragment(), result.result().getFragment());
          assertEquals(doActionResult.getTransition(), result.result().getTransition());
          assertJsonEquals(expectedLog, result.result().getLog());
        });
  }

  @Test
  @DisplayName("Expect exception in storage to be ignored and logged when flag configured")
  void saveFailureIgnoredWhenConfigured(VertxTestContext testContext) {
    lookupEmpty();
    storeFailing();
    FragmentResult doActionResult = successResult();
    Action tested = create(CACHE_KEY, doActionReturning(doActionResult), true, false);

    JsonObject expectedLog = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put("errors", new JsonArray()
                .add(new JsonObject().put("className", RuntimeException.class.getName()))));

    verifyActionResult(testContext, tested,
        result -> {
          assertEquals(doActionResult.getFragment(), result.result().getFragment());
          assertEquals(doActionResult.getTransition(), result.result().getTransition());
          assertJsonEquals(expectedLog, result.result().getLog());
        });
  }


  private Action mockedDoAction() {
    return mockedDoAction(doActionIdleWithLogs());
  }

  private Action mockedDoAction(Action wrapped) {
    Action doAction = mock(Action.class);
    lenient().doAnswer(invocation -> {
      wrapped.apply(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(doAction).apply(any(), any());
    return doAction;
  }

  private Action create(Action doAction) {
    return create(CACHE_KEY, doAction);
  }

  private Action create(String cacheKey, Action doAction) {
    return new CacheAction(
        ACTION_ALIAS,
        cacheKey,
        doAction,
        ActionLogLevel.INFO,
        lookup,
        store,
        true,
        true
    );
  }

  private Action create(String cacheKey, Action doAction, boolean failWhenLookupFails,
      boolean failWhenStoreFails) {
    return new CacheAction(
        ACTION_ALIAS,
        cacheKey,
        doAction,
        ActionLogLevel.INFO,
        lookup,
        store,
        failWhenLookupFails,
        failWhenStoreFails
    );
  }

  private void lookupSucceeding() {
    when(lookup.find(any(), any())).thenReturn(Maybe.just(CacheTestUtils.SOME_VALUE));
    when(lookup.toResponse(any(), eq((Object) CacheTestUtils.SOME_VALUE)))
        .thenReturn(FragmentResult.success(new Fragment("", new JsonObject(), "")
            .appendPayload(PAYLOAD_KEY, CacheTestUtils.SOME_VALUE)));
  }

  private void lookupEmpty() {
    when(lookup.find(any(), any())).thenReturn(Maybe.empty());
  }

  private void lookupFailing() {
    when(lookup.find(any(), any())).thenReturn(Maybe.error(new RuntimeException()));
  }

  private void storeFailing() {
    doThrow(RuntimeException.class).when(store).save(any(), any(), any());
  }

  private FragmentContext richContext() {
    return new FragmentContext(
        new Fragment("some-id", new JsonObject().put("p", "config"), "")
            .appendPayload("z", "payload"),
        new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add("x", "header"))
            .setParams(MultiMap.caseInsensitiveMultiMap().add("y", "param"))
    );
  }

}
