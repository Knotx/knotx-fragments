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

import io.knotx.fragments.handler.api.Knot;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

@Cacheable
public class KnotFactory implements ActionFactory {

  @Override
  public String getName() {
    return "knot";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx,
      Action doAction) {
    String address = config.getString("address");
    DeliveryOptions deliveryOptions = new DeliveryOptions(
        config.getJsonObject("deliveryOptions") == null ? new JsonObject()
            : config.getJsonObject("deliveryOptions"));

    return Knot.createProxyWithOptions(vertx, address, deliveryOptions);
  }

}
