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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.action.library.logging;

import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHED_VALUE;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_HIT;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_KEY;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_MISS;
import static io.knotx.fragments.action.library.InMemoryCacheAction.CACHE_PASS;
import static io.knotx.fragments.action.library.InMemoryCacheAction.COMPUTED_VALUE;
import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.library.InMemoryCacheActionFactory;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(KnotxExtension.class)
class InMemoryCacheActionFactoryLoggingTest {

  private static final String INFO = "info";
  private static final String ERROR = "error";

  private static final String EXAMPLE_CACHE_KEY = "cProduct";
  private static final String ACTION_ALIAS = "action";
  private static final String PAYLOAD_KEY = "product";
  private static final String LOG_LEVEL_KEY = "logLevel";
  private static final String LOGS_KEY = "logs";
  private static final String INVOCATIONS_LOGS_KEY = "doActionLogs";

  private static final JsonObject DO_ACTION_LOGS = new JsonObject()
      .put("alias", "some-do-action")
      .put("logs", new JsonObject()
          .put("InnerInfo", "InnerValue"))
      .put("doActionLogs", new JsonArray());

  private static final JsonObject PAYLOAD = new JsonObject().put("someKey", "someValue");

  private static final JsonObject ACTION_CONFIG = new JsonObject().put("payloadKey", PAYLOAD_KEY)
      .put("cacheKey", EXAMPLE_CACHE_KEY);

  @Test
  @DisplayName("Expect cache miss not logged on ERROR level")
  void cacheMissNotOnError(VertxTestContext testContext) {
    Action tested = cache(targetAppending(), ERROR);

    applyOnce(testContext, tested, log -> assertTrue(log.getJsonObject(LOGS_KEY).isEmpty()));
  }

  @Test
  @DisplayName("Expect cache hit not logged on ERROR level")
  void cacheHitNotOnError(VertxTestContext testContext) {
    Action tested = cache(targetAppending(), ERROR);

    applyTwice(testContext, tested,
        secondLog -> assertTrue(secondLog.getJsonObject(LOGS_KEY).isEmpty()));
  }

  @Test
  @DisplayName("Expect successful doAction's log not on ERROR level")
  void successfulDoActionLogsNotOnError(VertxTestContext testContext) {
    Action tested = cache(targetIdle(), ERROR);

    applyOnce(testContext, tested,
        log -> assertTrue(log.getJsonArray(INVOCATIONS_LOGS_KEY).isEmpty()));
  }

  @Test
  @DisplayName("Expect cache miss logged on INFO level")
  void cacheMissOnInfo(VertxTestContext testContext) {
    Action tested = cache(targetAppending(), INFO);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put(CACHE_MISS, new JsonObject()
                .put(COMPUTED_VALUE, PAYLOAD)));

    applyOnce(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @Test
  @DisplayName("Expect cache hit logged on INFO level")
  void cacheHitOnInfo(VertxTestContext testContext) {
    Action tested = cache(targetAppending(), INFO);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put(CACHE_HIT, new JsonObject()
                .put(CACHE_KEY, EXAMPLE_CACHE_KEY)
                .put(CACHED_VALUE, PAYLOAD)));

    applyTwice(testContext, tested, secondLog -> assertJsonEquals(expected, secondLog));
  }

  @Test
  @DisplayName("Expect successful doAction's log on INFO level")
  void successfulDoActionLogsOnInfo(VertxTestContext testContext) {
    Action tested = cache(targetIdle(), INFO);

    JsonObject expected = new JsonObject()
        .put(INVOCATIONS_LOGS_KEY, new JsonArray()
            .add(new JsonObject()
                .put("success", true)
                .put("doActionLog", DO_ACTION_LOGS)));

    applyOnce(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @ParameterizedTest
  @ValueSource(strings = {INFO, ERROR})
  @DisplayName("Expect failed doAction's log on INFO and ERROR levels")
  void failingDoActionLogsOnInfoAndError(String level, VertxTestContext testContext) {
    Action tested = cache(targetError(), level);

    JsonObject expected = new JsonObject()
        .put(INVOCATIONS_LOGS_KEY, new JsonArray()
            .add(new JsonObject()
                .put("success", false)
                .put("doActionLog", DO_ACTION_LOGS)));

    applyOnce(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @ParameterizedTest
  @ValueSource(strings = {INFO, ERROR})
  @DisplayName("Expect cache pass logged on INFO and ERROR levels")
  void cachePassOnInfoAndError(String level, VertxTestContext testContext) {
    Action tested = cache(targetIdle(), level);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put(CACHE_PASS, new JsonObject()
                .put(CACHE_KEY, EXAMPLE_CACHE_KEY)));

    applyOnce(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @ParameterizedTest
  @ValueSource(strings = {INFO, ERROR})
  @DisplayName("Expect doAction's exception logged on INFO and ERROR levels")
  void failingDoActionExceptionOnInfoAndError(String level, VertxTestContext testContext) {
    Action tested = cache(
        targetFatal(() -> new IllegalStateException("Application failed!")), level);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put("errors", new JsonArray()
                .add(new JsonObject()
                    .put("className", IllegalStateException.class.getCanonicalName())
                    .put("message", "Application failed!")
                )));

    applyOnce(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  private Action cache(Action doAction, String logLevel) {
    return new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, actionConfig(logLevel), null, doAction);
  }

  private static JsonObject actionConfig(String logLevel) {
    return ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, logLevel);
  }

  private Action targetFatal(Supplier<RuntimeException> generator) {
    return (fragmentContext, resultHandler) -> Future
        .<FragmentResult>failedFuture(generator.get())
        .onComplete(resultHandler);
  }

  private Action targetIdle() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      Future
          .succeededFuture(new FragmentResult(fragment, SUCCESS_TRANSITION, DO_ACTION_LOGS))
          .onComplete(resultHandler);
    };
  }

  private Action targetError() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      Future
          .succeededFuture(new FragmentResult(fragment, ERROR_TRANSITION, DO_ACTION_LOGS))
          .onComplete(resultHandler);
    };
  }

  private Action targetAppending() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(PAYLOAD_KEY, InMemoryCacheActionFactoryLoggingTest.PAYLOAD);
      Future
          .succeededFuture(new FragmentResult(fragment, SUCCESS_TRANSITION))
          .onComplete(resultHandler);
    };
  }

  private void applyOnce(VertxTestContext testContext, Action tested,
      Consumer<JsonObject> assertions) {
    tested.apply(someFragmentContext(), result -> testContext.verify(() -> {
      assertions.accept(result.result().getLog());
      testContext.completeNow();
    }));
  }

  private void applyTwice(VertxTestContext testContext, Action tested,
      Consumer<JsonObject> secondAssertions) {
    tested.apply(someFragmentContext(), firstResult -> tested
        .apply(someFragmentContext(), secondResult -> testContext.verify(() -> {
          secondAssertions.accept(secondResult.result().getLog());
          testContext.completeNow();
        })));
  }

  private FragmentContext someFragmentContext() {
    return new FragmentContext(new Fragment("type", new JsonObject(), "initial body"),
        new ClientRequest());
  }

}
