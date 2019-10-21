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

import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.options.GraphOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Inline body action factory class. It can be initialized with a configuration:
 * <pre>
 *   inlineBodyFallback {
 *     name = inline-body,
 *     config {
 *       body = "some static content"
 *     }
 *   }
 * </pre>
 * WARNING: This action modifies Fragment body so it should not be used in composite nodes
 * {@link GraphOptions#isComposite()}.
 */
@Cacheable
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
