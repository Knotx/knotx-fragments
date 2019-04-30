package io.knotx.fragments.handler.action;

import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Inline body action factory class. It can be initialized with a configuration:
 * <pre>
 *   inlineBodyFallback {
 *     name = inline-body,
 *     config {
 *       body = "<div>some static content</div>"
 *     }
 *   }
 * </pre>
 * WARNING: This action modifies Fragment body so it should not be used in composite nodes {@see
 * io.knotx.fragments.handler.options.NodeOptions#isComposite()}.
 */
public class InlineBodyActionFactory implements ActionFactory {

  private static final String DEFAULT_EMPTY_BODY = "";

  @Override
  public String getName() {
    return "inline-body";
  }

  /**
   * Creates inline body action that replaces Fragment body with static content.
   *
   * @param alias - action alias
   * @param config - JSON configuration
   * @param vertx - vertx instance
   * @param doAction - <pre>null</pre> value expected
   */
  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    if (doAction != null) {
      throw new IllegalArgumentException("Inline body action does not support doAction");
    }
    return (fragmentContext, resultHandler) -> {
      fragmentContext.getFragment()
          .setBody(config.getString("body", DEFAULT_EMPTY_BODY));
      Future<FragmentResult> resultFuture = Future.succeededFuture(
          new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION));
      resultFuture.setHandler(resultHandler);
    };
  }

}
