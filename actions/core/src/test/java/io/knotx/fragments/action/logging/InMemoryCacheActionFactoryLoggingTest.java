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
package io.knotx.fragments.action.logging;


import static io.knotx.fragments.action.api.log.ActionLogLevel.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.action.cache.memory.InMemoryCacheActionFactory;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(KnotxExtension.class)
class InMemoryCacheActionFactoryLoggingTest {

  private static final String CACHE_KEY = "cache_key";
  private static final String CACHED_VALUE = "cached_value";
  private static final String COMPUTED_VALUE = "computed_value";
  private static final String CACHE_MISS = "cache_miss";
  private static final String CACHE_HIT = "cache_hit";
  private static final String CACHE_PASS = "cache_pass";
  private static final String EXAMPLE_CACHE_KEY = "cProduct";
  private static final String ACTION_ALIAS = "action";
  private static final String PAYLOAD_KEY = "product";
  private static final String LOG_LEVEL_KEY = "logLevel";
  private static final String LOGS_KEY = "logs";
  private static final String DO_ACTION_LOGS_KEY = "doActionLogs";

  private static final JsonObject ACTION_CONFIG = new JsonObject().put("payloadKey", PAYLOAD_KEY)
      .put("cacheKey", EXAMPLE_CACHE_KEY);
  private static final JsonArray EMPTY_JSON_ARRAY = new JsonArray();

  private Fragment firstFragment;
  private Fragment secondFragment;

  @BeforeEach
  void setUp() {
    firstFragment = new Fragment("type", new JsonObject(), "initial body");
    secondFragment = new Fragment("type", new JsonObject(), "initial body");
  }

  @ParameterizedTest
  @MethodSource("provideAllLogLevelConfigurations")
  @DisplayName("Cache miss gets logged with computed value only when log level is info")
  void callActionWithPayloadUpdate(JsonObject configuration, boolean isLogLevelInfo,
      VertxTestContext testContext) throws Throwable {
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
        .create(ACTION_ALIAS, configuration, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getLog().getJsonObject(LOGS_KEY);
            if (isLogLevelInfo) {
              assertTrue(log.containsKey(CACHE_MISS));
              assertEquals(expectedPayloadValue,
                  log.getJsonObject(CACHE_MISS)
                      .getJsonObject(COMPUTED_VALUE));
            } else {
              assertFalse(log.containsKey(CACHE_MISS));
            }
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideAllLogLevelConfigurations")
  @DisplayName("Cache hit gets logged with cached value only when log level is info")
  void callActionTwiceWithTheSameKey(JsonObject configuration, boolean isLogLevelInfo,
      VertxTestContext testContext) throws Throwable {
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
        .create(ACTION_ALIAS, configuration, null, doAction);

    FragmentContext firstRequestContext = new FragmentContext(firstFragment, new ClientRequest());
    FragmentContext secondRequestContext = new FragmentContext(secondFragment, new ClientRequest());

    // when
    tested.apply(firstRequestContext,
        firstResult -> tested.apply(
            secondRequestContext, secondResult -> {
              // then
              testContext.verify(() -> {
                JsonObject log = secondResult.result().getLog().getJsonObject(LOGS_KEY);
                if (isLogLevelInfo) {
                  assertTrue(log.containsKey(CACHE_HIT));
                  assertEquals(expectedPayloadValue, log.getJsonObject(CACHE_HIT)
                      .getJsonObject(CACHED_VALUE));
                } else {
                  assertFalse(log.containsKey(CACHE_HIT));
                }
                testContext.completeNow();
              });
            })
    );

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideAllLogLevelConfigurations")
  @DisplayName("DoAction log is present when transition is _success and only on log level set to info")
  void callActionWithDoActionReturningSuccessTransition(JsonObject configuration,
      boolean isLogLevelInfo,
      VertxTestContext testContext) throws Throwable {

    ActionLogger innerActionLog = ActionLogger.create("DoAction", INFO);
    innerActionLog.info("InnerInfo", "InnerValue");

    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(
            new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION,
                innerActionLog.toLog().toJson()))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, configuration, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonArray calledDoActionLogEntires = result.result().getLog()
                .getJsonArray(DO_ACTION_LOGS_KEY);
            if (isLogLevelInfo) {
              JsonObject calledDoActionLogEntry = calledDoActionLogEntires.getJsonObject(0);
              assertTrue(calledDoActionLogEntry.getBoolean("success"));
              String innerInfoLogValue = calledDoActionLogEntry.getJsonObject("doActionLog")
                  .getJsonObject(LOGS_KEY)
                  .getString("InnerInfo");
              assertEquals("InnerValue", innerInfoLogValue);
            } else {
              assertTrue(calledDoActionLogEntires.isEmpty());
            }
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideAllLogLevelConfigurations")
  @DisplayName("DoAction log is present when transition is _error")
  void callActionWithDoActionReturningErrorTransition(JsonObject configuration,
      boolean isLogLevelInfo,
      VertxTestContext testContext) throws Throwable {

    ActionLogger innerActionLog = ActionLogger.create("DoAction", INFO);
    innerActionLog.info("InnerInfo", "InnerValue");

    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(
            new FragmentResult(fragmentContext.getFragment(), FragmentResult.ERROR_TRANSITION,
                innerActionLog.toLog().toJson()))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, configuration, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject calledActionLog = result.result().getLog()
                .getJsonArray(DO_ACTION_LOGS_KEY).getJsonObject(0);
            assertFalse(calledActionLog.getBoolean("success"));
            String innerInfoLogValue = calledActionLog.getJsonObject("doActionLog")
                .getJsonObject(LOGS_KEY)
                .getString("InnerInfo");
            assertEquals("InnerValue", innerInfoLogValue);
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideAllLogLevelConfigurations")
  @DisplayName("Cache pass gets logged when no payload is added by doAction")
  void callActionThatDoesNotAddPayload(JsonObject configuration, boolean isLogLevelInfo,
      VertxTestContext testContext)
      throws Throwable {
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(
            new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, configuration, null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getLog().getJsonObject(LOGS_KEY);
            assertTrue(log.containsKey(CACHE_PASS));
            assertEquals(EXAMPLE_CACHE_KEY, log.getJsonObject(CACHE_PASS).getString(CACHE_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @ParameterizedTest
  @MethodSource("provideAllLogLevelConfigurations")
  @DisplayName("Should preserve action logs when doAction has ended with failure")
  void callActionCausingFailure(JsonObject configuration, boolean isLogLevelInfo,
      VertxTestContext testContext) {
    Action doAction = (fragmentContext, resultHandler) -> Future
        .<FragmentResult>failedFuture(new IllegalStateException("Application failed!"))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, configuration, null, doAction);

    tested.apply(new FragmentContext(firstFragment, new ClientRequest()), result ->
        testContext.verify(() -> {
          JsonObject nodeLog = result.result().getLog();
          assertNotNull(nodeLog);
          JsonArray errors = nodeLog.getJsonObject("logs").getJsonArray("errors");
          JsonObject doActionError = errors.getJsonObject(0);
          assertEquals(EMPTY_JSON_ARRAY, nodeLog.getJsonArray("doActionLogs"));
          assertEquals(1, errors.getList().size());
          assertEquals(IllegalStateException.class.getCanonicalName(),
              doActionError.getString("className"));
          assertEquals("Application failed!", doActionError.getString("message"));
          testContext.completeNow();
        })
    );
  }

  private static Stream<Arguments> provideAllLogLevelConfigurations() {
    return Stream.of(
        Arguments.of(ACTION_CONFIG.copy(), false),
        Arguments.of(ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "error"), false),
        Arguments.of(ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "info"), true)
    );
  }
}
