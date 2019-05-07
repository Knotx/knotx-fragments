package io.knotx.fragments.handler.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragment.Fragment;
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
class InMemoryCacheActionFactoryTest {

  private static final String EXPECTED_PAYLOAD_DATA = "some content";
  private static final String PAYLOAD_KEY = "product";
  private static final String ACTION_ALIAS = "action";
  private Fragment fragment;
  private Fragment secondFragment;

  @BeforeEach
  void setUp() {
    fragment = new Fragment("type", new JsonObject(), "initial body");
    secondFragment = new Fragment("type", new JsonObject(),
        "initial body");
  }

  @DisplayName("Success result when doAction ends with _success transition.")
  @Test
  void callActionWithSuccessTransition(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("key", PAYLOAD_KEY), null, doAction);

    // when
    tested.apply(new FragmentContext(fragment, new ClientRequest()),
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

  @DisplayName("Success result with _error transition when doAction ends with _success transition.")
  @Test
  void callActionWithErrorTransition(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(fragment, FragmentResult.ERROR_TRANSITION))
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("key", PAYLOAD_KEY), null, doAction);

    // when
    tested.apply(new FragmentContext(fragment, new ClientRequest()),
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

  @DisplayName("Error result when doAction throws an exception")
  @Test
  void callActionThatThrowsException(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> Future
        .<FragmentResult>failedFuture(new IllegalStateException())
        .setHandler(resultHandler);

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("key", PAYLOAD_KEY), null, doAction);

    // when
    tested.apply(new FragmentContext(fragment, new ClientRequest()),
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

  @DisplayName("doAction invoked twice when cache is disabled (maximum size is 0)")
  @Test
  void callDoActionTwice(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      String value = uniqueValue();
      fragment.appendPayload(PAYLOAD_KEY, value);
      Future
          .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("key", PAYLOAD_KEY)
            .put("cache", new JsonObject().put("maximumSize", 0)), null, doAction);

    // when
    tested.apply(new FragmentContext(fragment, new ClientRequest()),
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

  @DisplayName("doAction invoked once when cache is enabled")
  @Test
  void callDoActionOnce(VertxTestContext testContext) throws Throwable {
    // given
    Action doAction = (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      String value = uniqueValue();
      fragment.appendPayload(PAYLOAD_KEY, value);
      Future
          .succeededFuture(new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION))
          .setHandler(resultHandler);
    };

    Action tested = new InMemoryCacheActionFactory()
        .create(ACTION_ALIAS, new JsonObject().put("key", PAYLOAD_KEY)
            .put("cache", new JsonObject()), null, doAction);

    // when
    tested.apply(new FragmentContext(fragment, new ClientRequest()),
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

  private String uniqueValue() {
    return EXPECTED_PAYLOAD_DATA + " [" + System.currentTimeMillis() + "]";
  }

}