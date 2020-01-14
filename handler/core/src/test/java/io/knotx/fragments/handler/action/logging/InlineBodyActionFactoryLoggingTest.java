package io.knotx.fragments.handler.action.logging;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.action.InlineBodyActionFactory;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class InlineBodyActionFactoryLoggingTest {

  private static final String LOGS_KEY = "logs";

  private static final String ACTION_ALIAS = "action";
  private static final String BODY_TO_INLINE = "body to inline";
  private static final String INITIAL_BODY = "initial body";

  @Test
  @DisplayName("Logs old and new body")
  void applyActionWithInfoLogLevel(VertxTestContext testContext) throws Throwable {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), INITIAL_BODY);
    Action action = new InlineBodyActionFactory().create(ACTION_ALIAS, new JsonObject().put("body",
        BODY_TO_INLINE).put("logLevel", "info"), null, null);

    // when
    action.apply(new FragmentContext(fragment, new ClientRequest()),
        result -> {
          // then
          testContext.verify(() -> {
            JsonObject logs = result.result().getNodeLog().getJsonObject(LOGS_KEY);
            Assertions.assertTrue(logs.containsKey(InlineBodyActionFactory.SUBSTITUTION_KEY));
            Assertions.assertEquals(INITIAL_BODY,
                logs.getJsonObject(InlineBodyActionFactory.SUBSTITUTION_KEY)
                    .getString(InlineBodyActionFactory.ORIGINAL_BODY_KEY));
            Assertions
                .assertEquals(BODY_TO_INLINE,
                    logs.getJsonObject(InlineBodyActionFactory.SUBSTITUTION_KEY)
                        .getString(InlineBodyActionFactory.BODY_KEY));
          });
          testContext.completeNow();
        });

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
