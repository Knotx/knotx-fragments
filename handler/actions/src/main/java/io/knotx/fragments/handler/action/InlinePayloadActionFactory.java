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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.engine.api.node.single.FragmentContext;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Cacheable
public class InlinePayloadActionFactory implements ActionFactory {

  private static final String ALIAS_KEY = "alias";
  private static final String PAYLOAD_KEY = "payload";

  private static final String KEY_LOG_KEY = "key";
  private static final String VALUE_LOG_KEY = "value";

  @Override
  public String getName() {
    return "inline-payload";
  }

  /**
   * Creates Inline Payload Action that puts JsonObject / JsonArray to Fragment payload with alias
   * key.
   *
   * @param alias - action alias
   * @param config - JSON configuration
   * @param vertx - vertx instance
   * @param doAction - <pre>null</pre> value expected
   */
  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    if (doAction != null) {
      throw new IllegalArgumentException("Inline Payload Action does not support doAction");
    }
    if (!config.containsKey(PAYLOAD_KEY)) {
      throw new IllegalArgumentException("Inline Payload Action requires payload parameter");
    }
    return (fragmentContext, resultHandler) -> {
      ActionLogLevel logLevel = ActionLogLevel.fromConfig(config, ActionLogLevel.ERROR);
      ActionLogger actionLogger = ActionLogger.create(alias, logLevel);
      String key = config.getString(ALIAS_KEY, alias);
      Object value = config.getMap().get(PAYLOAD_KEY);
      Fragment fragment = fragmentContext.getFragment();

      updatePayload(fragment, key, value, actionLogger);
      successTransition(fragmentContext, actionLogger, resultHandler);
    };
  }

  private void updatePayload(Fragment fragment, String key, Object value,
      ActionLogger actionLogger) {
    fragment.appendPayload(key, value);
    logSubstitution(actionLogger, key, value);
  }

  private void logSubstitution(ActionLogger actionLogger, String key, Object value) {
    actionLogger.info(KEY_LOG_KEY, key);
    actionLogger.info(VALUE_LOG_KEY, value);
  }

  private void successTransition(FragmentContext fragmentContext, ActionLogger actionLogger,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    Future<FragmentResult> resultFuture = Future.succeededFuture(
        new FragmentResult(fragmentContext.getFragment(), FragmentResult.SUCCESS_TRANSITION,
            actionLogger.toLog().toJson()));
    resultFuture.setHandler(resultHandler);
  }

}
