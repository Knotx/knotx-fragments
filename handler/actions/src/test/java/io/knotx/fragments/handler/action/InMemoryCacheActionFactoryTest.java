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
package io.knotx.fragments.handler.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.engine.api.node.single.FragmentContext;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class InMemoryCacheActionFactoryTest {

  private static final String EXPECTED_PAYLOAD_DATA = "some content";
  private static final String ACTION_ALIAS = "action";
  private static final String PAYLOAD_KEY = "product";

  private static final JsonObject ACTION_CONFIG = new JsonObject().put("payloadKey", PAYLOAD_KEY)
      .put("cacheKey", "cProduct");

  private Fragment firstFragment;
  private Fragment secondFragment;

  @BeforeEach
  void setUp() {
    firstFragment = new Fragment("type", new JsonObject(), "initial body");
    secondFragment = new Fragment("type", new JsonObject(), "initial body");
  }

  @DisplayName("Success result when doAction ends with _success transition.")
  @Test
  void callActionWithSuccessTransition(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(firstFragment, FragmentResult.SUCCESS_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> assertTrue(result.succeeded()));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("_error transition when doAction ends with _error transition.")
  @Test
  void callActionWithErrorTransition(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(firstFragment, FragmentResult.ERROR_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            assertTrue(result.succeeded());
            assertEquals(FragmentResult.ERROR_TRANSITION, result.result().getTransition());
          });
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Failed result when doAction throws an exception")
  @Test
  void callActionThatThrowsException(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .<FragmentResult>failedFuture(new IllegalStateException())
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> assertTrue(result.failed()));
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Payload contains data specified by payloadKey when doAction adds payloadKey.")
  @Test
  void callActionWithPayloadUpdate(VertxTestContext testContext) throws Throwable {
    // given
    JsonObject expectedPayloadValue = new JsonObject().put("someKey", "someValue");
    Action doAction = (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(PAYLOAD_KEY, expectedPayloadValue);
      Future
          .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject payload = result.result().getFragment().getPayload();
            assertTrue(payload.containsKey(PAYLOAD_KEY));
            assertEquals(expectedPayloadValue, payload.getJsonObject(PAYLOAD_KEY));
          });
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("doAction invoked twice when cache is disabled (maximum size is 0)")
  @Test
  void callDoActionTwice(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(PAYLOAD_KEY, uniqueValue(fragmentContext.hashCode()));
      Future
          .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG
            .put("cache", new JsonObject().put("maximumSize", 0)), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        firstResult -> tested.apply(new FragmentContext(secondFragment, new ClientRequest()),
            secondResult -> {
              testContext.verify(() -> assertNotEquals(
                  firstResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY),
                  secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
              testContext.completeNow();
            }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("doAction invoked once when cache is enabled and cacheKeys are the same")
  @Test
  void callDoActionOnce(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(PAYLOAD_KEY, uniqueValue(fragmentContext.hashCode()));
      Future
          .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG
            .put("cache", new JsonObject()), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        firstResult -> tested.apply(new FragmentContext(secondFragment, new ClientRequest()),
            secondResult -> {
              testContext.verify(() -> assertEquals(
                  firstResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY),
                  secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
              testContext.completeNow();
            }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Different payload values when cache key uses requests data that are different.")
  @Test
  void callActionsDifferentCacheKeys(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(PAYLOAD_KEY, uniqueValue(fragmentContext.hashCode()));
      Future
          .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject()
                .put("payloadKey", PAYLOAD_KEY)
                .put("cacheKey", "product-{param.id}"),
            null, doAction);

    // when
    FragmentContext firstRequestContext = new FragmentContext(firstFragment,
        new ClientRequest().setParams(MultiMap.caseInsensitiveMultiMap().add("id", "product1")));
    FragmentContext secondRequestContext = new FragmentContext(secondFragment,
        new ClientRequest().setParams(MultiMap.caseInsensitiveMultiMap().add("id", "product2")));

    tested.apply(firstRequestContext,
        firstResult -> tested.apply(secondRequestContext,
            secondResult -> {
              testContext.verify(() -> assertNotEquals(
                  firstResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY),
                  secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
              testContext.completeNow();
            }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Error not cached.")
  @Test
  void callDoActionWithErrorAndDoActionWithSuccess(VertxTestContext testContext) throws Throwable {
    // given
    JsonObject expectedPayloadValue = new JsonObject().put("someKey", "someValue");
    Action doAction = (fragmentContext, resultHandler) -> {
      if (fragmentContext.getClientRequest().getParams().contains("error")) {
        Future
            .<FragmentResult>failedFuture(new IllegalStateException())
            .setHandler(resultHandler);
      } else {
        Fragment fragment = fragmentContext.getFragment();
        fragment.appendPayload(PAYLOAD_KEY, expectedPayloadValue);
        Future
            .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
            .setHandler(resultHandler);
      }
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS,
            new JsonObject()
                .put("payloadKey", PAYLOAD_KEY)
                .put("cacheKey", "product"),
            null, doAction);

    // when
    FragmentContext errorRequestContext = new FragmentContext(firstFragment,
        new ClientRequest().setParams(MultiMap.caseInsensitiveMultiMap().add("error", "expected")));
    FragmentContext successRequestContext = new FragmentContext(secondFragment, new ClientRequest());

    tested.apply(errorRequestContext,
        firstResult -> tested.apply(successRequestContext,
            secondResult -> {
              testContext.verify(() -> assertEquals(expectedPayloadValue,
                  secondResult.result().getFragment().getPayload().getMap().get(PAYLOAD_KEY)));
              testContext.completeNow();
            }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private String uniqueValue(int contextHash) {
    return EXPECTED_PAYLOAD_DATA + " [" + UUID.randomUUID().toString() + "|" + contextHash + "]";
  }

}
