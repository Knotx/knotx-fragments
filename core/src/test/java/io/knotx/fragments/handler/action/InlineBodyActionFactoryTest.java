package io.knotx.fragments.handler.action;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class InlineBodyActionFactoryTest {

  private static final String EXPECTED_VALUE = "expected value";
  private static final String INITIAL_BODY = "initial body";

  @Test
  @DisplayName("Expect not empty Fragment body when Action configuration specifies body.")
  void applyAction(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create("action", new JsonObject().put("body",
        EXPECTED_VALUE), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            Assertions.assertTrue(result.succeeded());
            Assertions.assertEquals(EXPECTED_VALUE, result.result().getFragment().getBody());
          });
          testContext.completeNow();
        });

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  @DisplayName("Expect empty Fragment body when Action configuration does not specify body.")
  void applyActionWithEmptyConfiguration(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create("action", new JsonObject(), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            Assertions.assertTrue(result.succeeded());
            Assertions.assertEquals("", result.result().getFragment().getBody());
          });
          testContext.completeNow();
        });

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}