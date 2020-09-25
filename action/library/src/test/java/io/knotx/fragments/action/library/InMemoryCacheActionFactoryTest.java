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
package io.knotx.fragments.action.library;

import static io.knotx.fragments.action.api.log.ActionLogger.ERRORS;
import static io.knotx.fragments.action.library.TestUtils.ACTION_ALIAS;
import static io.knotx.fragments.action.library.TestUtils.someContext;
import static io.knotx.fragments.action.library.TestUtils.someFragment;
import static io.knotx.fragments.action.library.TestUtils.successResult;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static io.knotx.fragments.action.library.TestUtils.verifyTwoActionResults;
import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentOperationError;
import io.knotx.fragments.api.FragmentOperationFailure;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
class InMemoryCacheActionFactoryTest {

  private static final String EXPECTED_PAYLOAD_DATA = "some content";
  private static final String PAYLOAD_KEY = "product";

  private static final JsonObject ACTION_CONFIG = new JsonObject()
      .put("payloadKey", PAYLOAD_KEY)
      .put("cacheKey", "cProduct");

  @DisplayName("Success result when doAction ends with _success transition.")
  @Test
  void callActionWithSuccessTransition(VertxTestContext testContext) {
    // given
    Action doAction = (SyncAction) fragmentContext -> successResult();

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy(), null, doAction);

    // when
    verifyActionResult(testContext, tested,
        result -> assertTrue(result.succeeded()));
  }

  @DisplayName("_error transition when doAction ends with _error transition.")
  @Test
  void callActionWithErrorTransition(VertxTestContext testContext) {
    // given
    Action doAction = (SyncAction) fragmentContext -> FragmentResult
        .fail(someFragment(), "code", "message");

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy(), null, doAction);

    // when, then
    verifyActionResult(testContext, tested, result -> {
      assertTrue(result.succeeded());
      assertEquals(ERROR_TRANSITION, result.result().getTransition());
    });
  }

  @DisplayName("Failed result when doAction throws an exception")
  @Test
  void callActionThatThrowsException(VertxTestContext testContext) {
    // given
    Action doAction = (context, handler) -> {
      throw new IllegalStateException();
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy(), null, doAction);

    // when, then
    verifyActionResult(testContext, tested, result -> {
      assertEquals(ERROR_TRANSITION, result.result().getTransition());
      List<FragmentOperationError> errors = result.result().getError().getExceptions();
      assertEquals(1, errors.size());
      assertEquals(IllegalStateException.class.getCanonicalName(),
          errors.get(0).getClassName());
    });
  }

  @DisplayName("Payload contains data specified by payloadKey when doAction adds payloadKey.")
  @Test
  void callActionWithPayloadUpdate(VertxTestContext testContext) {
    // given
    JsonObject expectedPayloadValue = new JsonObject().put("someKey", "someValue");
    Action doAction = (SyncAction) fragmentContext -> FragmentResult
        .success(fragmentContext.getFragment().appendPayload(PAYLOAD_KEY, expectedPayloadValue));

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy(), null, doAction);

    // when, then
    verifyActionResult(testContext, tested, result -> {
      JsonObject payload = result.result().getFragment().getPayload();
      assertTrue(payload.containsKey(PAYLOAD_KEY));
      assertEquals(expectedPayloadValue, payload.getJsonObject(PAYLOAD_KEY));
    });
  }

  @DisplayName("doAction invoked twice when cache is disabled (maximum size is 0)")
  @Test
  void callDoActionTwice(VertxTestContext testContext) {
    // given
    Action doAction = (SyncAction) fragmentContext -> FragmentResult
        .success(fragmentContext.getFragment()
            .appendPayload(PAYLOAD_KEY, uniqueValue(fragmentContext.hashCode())));

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy()
            .put("cache", new JsonObject().put("maximumSize", 0)), null, doAction);

    // when
    verifyTwoActionResults(testContext, tested, someContext(), someContext(),
        (firstResult, secondResult) -> assertNotEquals(
            firstResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY),
            secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
  }

  @DisplayName("doAction invoked once when cache is enabled and cacheKeys are the same")
  @Test
  void callDoActionOnce(VertxTestContext testContext) {
    // given
    Action doAction = (SyncAction) fragmentContext -> FragmentResult.success(
        fragmentContext.getFragment()
            .appendPayload(PAYLOAD_KEY, uniqueValue(fragmentContext.hashCode())));

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy()
            .put("cache", new JsonObject()), null, doAction);

    // when, then
    verifyTwoActionResults(testContext, tested, someContext(), someContext(),
        (firstResult, secondResult) -> assertEquals(
            firstResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY),
            secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
  }

  @DisplayName("Different payload values when cache key uses requests data that are different.")
  @Test
  void callActionsDifferentCacheKeys(VertxTestContext testContext) {
    // given
    Action doAction = (SyncAction) fragmentContext -> FragmentResult.success(
        fragmentContext.getFragment()
            .appendPayload(PAYLOAD_KEY, uniqueValue(fragmentContext.hashCode())));

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject()
                .put("payloadKey", PAYLOAD_KEY)
                .put("cacheKey", "product-{param.id}"),
            null, doAction);

    // when
    FragmentContext firstRequestContext = new FragmentContext(someFragment(),
        new ClientRequest().setParams(MultiMap.caseInsensitiveMultiMap().add("id", "product1")));
    FragmentContext secondRequestContext = new FragmentContext(someFragment(),
        new ClientRequest().setParams(MultiMap.caseInsensitiveMultiMap().add("id", "product2")));

    verifyTwoActionResults(testContext, tested, firstRequestContext, secondRequestContext,
        (firstResult, secondResult) -> assertNotEquals(
            firstResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY),
            secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
  }

  @DisplayName("Error not cached.")
  @Test
  void callDoActionWithErrorAndDoActionWithSuccess(VertxTestContext testContext) {
    // given
    JsonObject expectedPayloadValue = new JsonObject().put("someKey", "someValue");
    Action doAction = (SyncAction) fragmentContext -> {
      if (fragmentContext.getClientRequest().getParams().contains("error")) {
        throw new IllegalStateException();
      } else {
        return FragmentResult.success(
            fragmentContext.getFragment().appendPayload(PAYLOAD_KEY, expectedPayloadValue));
      }
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject()
                .put("payloadKey", PAYLOAD_KEY)
                .put("cacheKey", "product"),
            null, doAction);

    // when
    FragmentContext errorRequestContext = new FragmentContext(someFragment(),
        new ClientRequest().setParams(MultiMap.caseInsensitiveMultiMap().add("error", "expected")));
    FragmentContext successRequestContext = new FragmentContext(someFragment(),
        new ClientRequest());

    verifyTwoActionResults(testContext, tested, errorRequestContext, successRequestContext,
        (firstResult, secondResult) -> assertEquals(expectedPayloadValue,
            secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
  }

  private String uniqueValue(int contextHash) {
    return EXPECTED_PAYLOAD_DATA + " [" + UUID.randomUUID().toString() + "|" + contextHash + "]";
  }

}
