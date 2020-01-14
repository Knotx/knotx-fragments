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
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Cacheable
public class InlinePayloadActionFactory implements ActionFactory {

  private static final String LOG_LEVEL_KEY = "logLevel";
  private static final String ALIAS_KEY = "alias";
  private static final String PAYLOAD_KEY = "payload";

  public static final String INLINE_LOG_KEY = "inline";
  public static final String KEY_LOG_KEY = "key";
  public static final String PAYLOAD_LOG_KEY = "payload";

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
      ActionLogger actionLogger = createLogger(alias, config);
      String key = config.getString(ALIAS_KEY, alias);
      Object payload = config.getMap().get(PAYLOAD_KEY);

      Future<FragmentResult> resultFuture = Future
          .succeededFuture(toResult(fragmentContext, key, payload, actionLogger));
      resultFuture.setHandler(resultHandler);
    };
  }

  private FragmentResult toResult(FragmentContext fragmentContext, String key, Object payload,
      ActionLogger actionLogger) {
    Fragment fragment = fragmentContext.getFragment();
    fragment.appendPayload(key, payload);
    logSubstitution(actionLogger, key, payload);
    return new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION,
        actionLogger.toLog().toJson());
  }

  private ActionLogger createLogger(String alias, JsonObject config) {
    String logLevel = config.containsKey(LOG_LEVEL_KEY) ? config.getString(LOG_LEVEL_KEY) :
        ActionLogLevel.ERROR.getLevel();
    return ActionLogger.create(alias, logLevel);
  }

  private void logSubstitution(ActionLogger actionLogger, String key, Object payload) {
    actionLogger.info(
        INLINE_LOG_KEY, new JsonObject().put(KEY_LOG_KEY, key).put(PAYLOAD_LOG_KEY, payload));
  }

}
