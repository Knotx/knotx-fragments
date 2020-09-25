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
package io.knotx.fragments.action.library.logging;

import static io.knotx.fragments.action.api.log.ActionInvocationLog.LOG;
import static io.knotx.fragments.action.api.log.ActionInvocationLog.SUCCESS;
import static io.knotx.fragments.action.api.log.ActionLog.INVOCATIONS;
import static io.knotx.fragments.action.library.TestUtils.someContext;
import static io.knotx.fragments.action.library.TestUtils.verifyActionResult;
import static io.knotx.fragments.action.library.TestUtils.verifyDeliveredResult;
import static io.knotx.fragments.action.library.TestUtils.verifyTwoActionResults;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.ACTION_LOG;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.LOGS_KEY;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.PAYLOAD_KEY;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.SOME_VALUE;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionAppending;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionError;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionFailed;
import static io.knotx.fragments.action.library.cache.CacheTestUtils.doActionIdleWithLogs;
import static io.knotx.fragments.action.library.cache.operations.CacheActionLogger.CACHED_VALUE;
import static io.knotx.fragments.action.library.cache.operations.CacheActionLogger.CACHE_HIT;
import static io.knotx.fragments.action.library.cache.operations.CacheActionLogger.CACHE_KEY;
import static io.knotx.fragments.action.library.cache.operations.CacheActionLogger.CACHE_MISS;
import static io.knotx.fragments.action.library.cache.operations.CacheActionLogger.CACHE_PASS;
import static io.knotx.fragments.action.library.cache.operations.CacheActionLogger.COMPUTED_VALUE;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.library.InMemoryCacheActionFactory;
import io.knotx.fragments.api.FragmentOperationError;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(VertxExtension.class)
class InMemoryCacheActionFactoryLoggingTest {

  private static final String INFO = "info";
  private static final String ERROR = "error";

  private static final String EXAMPLE_CACHE_KEY = "cProduct";
  private static final String ACTION_ALIAS = "action";
  private static final String LOG_LEVEL_KEY = "logLevel";

  private static final JsonObject ACTION_CONFIG = new JsonObject()
      .put("payloadKey", PAYLOAD_KEY)
      .put("cacheKey", EXAMPLE_CACHE_KEY);

  @Test
  @DisplayName("Expect cache miss not logged on ERROR level")
  void cacheMissNotOnError(VertxTestContext testContext) {
    Action tested = create(doActionAppending(), ERROR);

    verifyLog(testContext, tested, log -> assertTrue(log.getJsonObject(LOGS_KEY).isEmpty()));
  }

  @Test
  @DisplayName("Expect cache hit not logged on ERROR level")
  void cacheHitNotOnError(VertxTestContext testContext) {
    Action tested = create(doActionAppending(), ERROR);

    verifyLog(testContext, tested,
        secondLog -> assertTrue(secondLog.getJsonObject(LOGS_KEY).isEmpty()));
  }

  @Test
  @DisplayName("Expect successful doAction's log not on ERROR level")
  void successfulDoActionLogsNotOnError(VertxTestContext testContext) {
    Action tested = create(doActionIdleWithLogs(), ERROR);

    verifyLog(testContext, tested,
        log -> assertTrue(log.getJsonArray(INVOCATIONS).isEmpty()));
  }

  @Test
  @DisplayName("Expect cache miss logged on INFO level")
  void cacheMissOnInfo(VertxTestContext testContext) {
    Action tested = create(doActionAppending(), INFO);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put(CACHE_MISS, new JsonObject()
                .put(COMPUTED_VALUE, SOME_VALUE)));

    verifyLog(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @Test
  @DisplayName("Expect cache hit logged on INFO level")
  void cacheHitOnInfo(VertxTestContext testContext) {
    Action tested = create(doActionAppending(), INFO);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put(CACHE_HIT, new JsonObject()
                .put(CACHE_KEY, EXAMPLE_CACHE_KEY)
                .put(CACHED_VALUE, SOME_VALUE)));

    verifySecondLog(testContext, tested, secondLog -> assertJsonEquals(expected, secondLog));
  }

  @Test
  @DisplayName("Expect successful doAction's log on INFO level")
  void successfulDoActionLogsOnInfo(VertxTestContext testContext) {
    Action tested = create(doActionIdleWithLogs(), INFO);

    JsonObject expected = new JsonObject()
        .put(INVOCATIONS, new JsonArray()
            .add(new JsonObject()
                .put(SUCCESS, true)
                .put(LOG, ACTION_LOG)));

    verifyLog(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @ParameterizedTest
  @ValueSource(strings = {INFO, ERROR})
  @DisplayName("Expect failed doAction's log on INFO and ERROR levels")
  void failingDoActionLogsOnInfoAndError(String level, VertxTestContext testContext) {
    Action tested = create(doActionError(), level);

    JsonObject expected = new JsonObject()
        .put(INVOCATIONS, new JsonArray()
            .add(new JsonObject()
                .put(SUCCESS, false)
                .put(LOG, ACTION_LOG)));

    verifyLog(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @ParameterizedTest
  @ValueSource(strings = {INFO, ERROR})
  @DisplayName("Expect cache pass logged on INFO and ERROR levels")
  void cachePassOnInfoAndError(String level, VertxTestContext testContext) {
    Action tested = create(doActionIdleWithLogs(), level);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put(CACHE_PASS, new JsonObject()
                .put(CACHE_KEY, EXAMPLE_CACHE_KEY)));

    verifyLog(testContext, tested, log -> assertJsonEquals(expected, log));
  }

  @ParameterizedTest
  @ValueSource(strings = {INFO, ERROR})
  @DisplayName("Expect doAction's exception logged on INFO and ERROR levels")
  void failingDoActionExceptionOnInfoAndError(String level, VertxTestContext testContext) {
    Action tested = create(
        doActionFailed(() -> new IllegalStateException("Application failed!")), level);

    JsonObject expected = new JsonObject()
        .put(LOGS_KEY, new JsonObject()
            .put("errors", new JsonArray()
                .add(new JsonObject()
                    .put("className", IllegalStateException.class.getCanonicalName())
                    .put("message", "Application failed!")
                )));

    verifyDeliveredResult(testContext, tested, result -> {
      List<FragmentOperationError> errors = result.getError().getExceptions();
      assertEquals(1, errors.size());
      assertEquals(IllegalStateException.class.getCanonicalName(), errors.get(0).getClassName());
      assertEquals("Application failed!", errors.get(0).getMessage());
    });
  }

  private Action create(Action doAction, String logLevel) {
    return new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, actionConfig(logLevel), null, doAction);
  }

  private static JsonObject actionConfig(String logLevel) {
    return ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, logLevel);
  }

  private void verifyLog(VertxTestContext testContext, Action tested,
      Consumer<JsonObject> assertions) {
    verifyActionResult(testContext, tested, result -> assertions.accept(result.result().getLog()));
  }

  private void verifySecondLog(VertxTestContext testContext, Action tested,
      Consumer<JsonObject> secondAssertions) {
    verifyTwoActionResults(testContext, tested, someContext(), someContext(),
        (firstResult, secondResult) -> secondAssertions.accept(secondResult.result().getLog()));
  }

}
