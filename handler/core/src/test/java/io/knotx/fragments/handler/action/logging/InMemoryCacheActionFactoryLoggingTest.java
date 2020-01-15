package io.knotx.fragments.handler.action.logging;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.action.InMemoryCacheActionFactory;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class InMemoryCacheActionFactoryLoggingTest {

  private static final String CACHE_LOOKUP_KEY = "cache_lookup";
  private static final String CACHE_MISS_KEY = "cache_miss";
  private static final String CACHE_HIT_KEY = "cache_hit";
  private static final String CACHE_PASS_KEY = "cache_pass";
  private static final String TRANSITION_KEY = "transition";

  private static final String EXAMPLE_CACHE_KEY = "cProduct";

  private static final String ACTION_ALIAS = "action";
  private static final String PAYLOAD_KEY = "product";
  private static final String LOG_LEVEL_KEY = "logLevel";
  private static final String LOGS_KEY = "logs";

  private static final JsonObject ACTION_CONFIG = new JsonObject().put("payloadKey", PAYLOAD_KEY)
      .put("cacheKey", EXAMPLE_CACHE_KEY);

  private Fragment firstFragment;
  private Fragment secondFragment;

  @BeforeEach
  void setUp() {
    firstFragment = new Fragment("type", new JsonObject(), "initial body");
    secondFragment = new Fragment("type", new JsonObject(), "initial body");
  }

  @DisplayName("Cache lookup gets logged with cache key when log level is info")
  @Test
  void callActionWithSuccessTransitionLog(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(firstFragment, FragmentResult.SUCCESS_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "info"), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            assertTrue(log.containsKey(CACHE_LOOKUP_KEY));
            assertEquals(EXAMPLE_CACHE_KEY, log.getString(CACHE_LOOKUP_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }


  @DisplayName("Cache miss gets logged with computed value when log level is info")
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
        .create(ACTION_ALIAS, ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "info"), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            assertTrue(log.containsKey(CACHE_MISS_KEY));
            assertEquals(expectedPayloadValue,
                log.getJsonObject(CACHE_MISS_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Cache hit gets logged with cached value when log level is info")
  @Test
  void callActionTwiceWithTheSameKey(VertxTestContext testContext) throws Throwable {
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
        .create(ACTION_ALIAS, ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "info"), null, doAction);

    FragmentContext firstRequestContext = new FragmentContext(firstFragment, new ClientRequest());
    FragmentContext secondRequestContext = new FragmentContext(secondFragment, new ClientRequest());

    // when
    tested.apply(firstRequestContext,
        firstResult -> tested.apply(
            secondRequestContext, secondResult -> {
              // then
              testContext.verify(() -> {
                JsonObject log = secondResult.result().getNodeLog().getJsonObject(LOGS_KEY);
                assertTrue(log.containsKey(CACHE_HIT_KEY));
                assertEquals(expectedPayloadValue,
                    log.getJsonObject(CACHE_HIT_KEY));
                testContext.completeNow();
              });
            })
    );

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Cache pass gets logged on _error transition from doAction when log level is info")
  @Test
  void callActionWithErrorTransition(VertxTestContext testContext) throws Throwable {
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(
            new FragmentResult(fragmentContext.getFragment(), FragmentResult.ERROR_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "info"), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            assertTrue(log.containsKey(CACHE_PASS_KEY));
            assertEquals(FragmentResult.ERROR_TRANSITION,
                log.getJsonObject(CACHE_PASS_KEY)
                    .getString(TRANSITION_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Cache pass gets logged when no payload is added by doAction when log level is info")
  @Test
  void callActionThatDoesNotAddPayload(VertxTestContext testContext) throws Throwable {
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(
            new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "info"), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            assertTrue(log.containsKey(CACHE_PASS_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }


  @DisplayName("Cache operations are not logged when log level is not set")
  @Test
  void callActionWithSuccessTransitionWithLogLevelNotSet(VertxTestContext testContext)
      throws Throwable {
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
          testContext.verify(() -> {
            JsonObject log = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            assertFalse(log.containsKey(CACHE_LOOKUP_KEY));
            assertFalse(log.containsKey(CACHE_HIT_KEY));
            assertFalse(log.containsKey(CACHE_MISS_KEY));
            assertFalse(log.containsKey(CACHE_PASS_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @DisplayName("Cache operations are not logged when log level is error")
  @Test
  void callActionWithSuccessTransitionWithLogLevelError(VertxTestContext testContext)
      throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(firstFragment, FragmentResult.SUCCESS_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, ACTION_CONFIG.copy().put(LOG_LEVEL_KEY, "error"), null, doAction);

    // when
    tested.apply(new FragmentContext(firstFragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject log = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            assertFalse(log.containsKey(CACHE_LOOKUP_KEY));
            assertFalse(log.containsKey(CACHE_HIT_KEY));
            assertFalse(log.containsKey(CACHE_MISS_KEY));
            assertFalse(log.containsKey(CACHE_PASS_KEY));
            testContext.completeNow();
          });
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
