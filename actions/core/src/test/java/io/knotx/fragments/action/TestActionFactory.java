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
package io.knotx.fragments.action;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Only for testing purposes.
 */
public class TestActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "test";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    return (fragmentContext, resultHandler) -> Future
        .succeededFuture(new FragmentResult(fragmentContext.getFragment(), "test"))
        .setHandler(resultHandler);
  }
}
