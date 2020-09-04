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

import static io.knotx.fragments.action.library.cache.TestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.cache.TestUtils.CACHE_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.DO_ACTION_LOGS;
import static io.knotx.fragments.action.library.cache.TestUtils.INVOCATIONS_LOGS_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.LOGS_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.PAYLOAD_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.SOME_VALUE;
import static io.knotx.fragments.action.library.cache.TestUtils.doActionAppending;
import static io.knotx.fragments.action.library.cache.TestUtils.doActionFatal;
import static io.knotx.fragments.action.library.cache.TestUtils.doActionIdle;
import static io.knotx.fragments.action.library.cache.TestUtils.doActionReturning;
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
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(KnotxExtension.class)
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
    Action tested = create(KEY_WITH_PLACEHOLDERS, doActionIdle());

    apply(testContext, tested,
        result -> verify(lookup, times(1)).find(eq(KEY_INTERPOLATED), any()));
  }

  @Test
  @DisplayName("Expect value returned from lookup is populated")
  void valueFromCachePopulated(VertxTestContext testContext) {
    lookupSucceeding();
    Action tested = create(doActionIdle());

    apply(testContext, tested,
        result -> assertEquals(SOME_VALUE, result.result().getFragment().getPayload()
            .getJsonObject(PAYLOAD_KEY)));
  }

  @Test
  @DisplayName("Expect doAction is called when no value in lookup")
  void doActionCalledWhen(VertxTestContext testContext) {
    lookupEmpty();
    Action doAction = mockedDoAction();
    Action tested = create(doAction);

    apply(testContext, tested,
        result -> verify(doAction, times(1)).apply(any(), any()));
  }

  @Test
  @DisplayName("Expect doAction call is logged")
  void doActionCallLogged(VertxTestContext testContext) {
    lookupEmpty();
    Action tested = create(doActionIdle());

    JsonObject expected = new JsonObject()
        .put(INVOCATIONS_LOGS_KEY, new JsonArray()
            .add(new JsonObject()
                .put("success", true)
                .put("doActionLog", DO_ACTION_LOGS)));

    apply(testContext, tested,
        result -> assertJsonEquals(expected, result.result().getLog()));
  }

  @Test
  @DisplayName("Expect doAction's result is passed to be stored")
  void doActionResultStored(VertxTestContext testContext) {
    lookupEmpty();
    FragmentResult returned = successResult();
    Action tested = create(doActionReturning(returned));

    apply(testContext, tested,
        result -> verify(store, times(1)).save(any(), eq(CACHE_KEY), eq(returned)));
  }

  @Test
  @DisplayName("Expect doAction's result fragment and transition are returned")
  void doActionResultPassed(VertxTestContext testContext) {
    lookupEmpty();
    FragmentResult returned = successResult();
    Action tested = create(doActionReturning(returned));

    apply(testContext, tested,
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

    apply(testContext, tested,
        result -> verify(doAction, times(0)).apply(any(), any()));
  }

  @Test
  @DisplayName("Expect throwing doAction prevents from storing information")
  void doActionFailurePreventsStoring(VertxTestContext testContext) {
    lookupEmpty();
    Action tested = create(doActionFatal(RuntimeException::new));

    apply(testContext, tested,
        result -> verify(store, times(0)).save(any(), any(), any()));
  }

  @Test
  @DisplayName("Expect lookup failure to result in error transition with original Fragment")
  void lookupFailureEndsInErrorTransition(VertxTestContext testContext) {
    lookupFailing();
    FragmentContext original = richContext();
    FragmentContext copy = new FragmentContext(original.toJson());
    Action tested = create(doActionIdle());

    apply(testContext, tested, copy,
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
    Action tested = create(doActionFatal(RuntimeException::new));

    apply(testContext, tested, copy,
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

    apply(testContext, tested, copy,
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

    apply(testContext, tested,
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

    apply(testContext, tested,
        result -> {
          assertEquals(doActionResult.getFragment(), result.result().getFragment());
          assertEquals(doActionResult.getTransition(), result.result().getTransition());
          assertJsonEquals(expectedLog, result.result().getLog());
        });
  }


  private Action mockedDoAction() {
    return mockedDoAction(doActionIdle());
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

  private void apply(VertxTestContext testContext, Action action, FragmentContext context,
      Consumer<AsyncResult<FragmentResult>> assertions) {
    action.apply(context, result ->
        testContext.verify(() -> {
          assertions.accept(result);
          testContext.completeNow();
        }));
  }

  private void apply(VertxTestContext testContext, Action action,
      Consumer<AsyncResult<FragmentResult>> assertions) {
    apply(testContext, action, richContext(), assertions);
  }

  private void lookupSucceeding() {
    when(lookup.find(any(), any())).thenReturn(Maybe.just(TestUtils.SOME_VALUE));
    when(lookup.toResponse(any(), eq((Object) TestUtils.SOME_VALUE)))
        .thenReturn(FragmentResult.success(new Fragment("", new JsonObject(), "")
            .appendPayload(PAYLOAD_KEY, TestUtils.SOME_VALUE)));
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

  private FragmentResult successResult() {
    return FragmentResult.success(new Fragment("", new JsonObject(), ""));
  }

}
