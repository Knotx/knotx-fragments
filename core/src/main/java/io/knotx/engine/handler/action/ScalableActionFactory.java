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
package io.knotx.engine.handler.action;

import io.knotx.engine.api.fragment.CacheableAction;
import io.knotx.engine.api.fragment.Action;
import io.knotx.engine.api.fragment.ActionFactory;
import io.knotx.engine.api.fragment.ScalableAction;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

@CacheableAction
public class ScalableActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "eb";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx,
      Action doAction) {
    String address = config.getString("address");
    DeliveryOptions deliveryOptions = new DeliveryOptions(
        config.getJsonObject("deliveryOptions") == null ? new JsonObject()
            : config.getJsonObject("deliveryOptions"));

    return ScalableAction.create(vertx, address, deliveryOptions);
  }

}
