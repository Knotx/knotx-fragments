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
package io.knotx.fragments.handler.action.http;

import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class HttpActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "http";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    // TODO add custom exception here (ActionConfigurationException)
    if (doAction != null) {
      throw new IllegalArgumentException("Http Action can not wrap another action");
    }
    return new HttpAction(vertx, new HttpActionOptions(config), alias,
        ActionLogLevel.fromConfig(config));
  }

}
