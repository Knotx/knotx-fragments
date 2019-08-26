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
import io.knotx.fragments.handler.api.ActionConfig;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Cacheable
public class InlinePayloadActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "inline-payload";
  }

  /**
   * Creates Inline Payload Action that puts JsonObject / JsonArray to Fragment payload with alias
   * key.
   *
   * @param config - action config
   * @param vertx - vertx instance
   */
  @Override
  public Action create(ActionConfig config, Vertx vertx) {
    if (config.hasAction()) {
      throw new IllegalArgumentException("Inline Payload Action does not support doAction");
    }
    JsonObject options = config.getOptions();
    if (!options.containsKey("payload")) {
      throw new IllegalArgumentException("Inline Payload Action requires payload parameter");
    }
    return (fragmentContext, resultHandler) -> {
      String key = options.getString("alias", config.getAlias());
      Object payload = options.getMap().get("payload");

      Future<FragmentResult> resultFuture = Future
          .succeededFuture(toResult(fragmentContext, key, payload));
      resultFuture.setHandler(resultHandler);
    };
  }

  private FragmentResult toResult(FragmentContext fragmentContext, String key, Object payload) {
    Fragment fragment = fragmentContext.getFragment();
    fragment.appendPayload(key, payload);
    return new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
  }

}
