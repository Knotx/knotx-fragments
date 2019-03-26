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
package io.knotx.engine.handler.proxy;

import io.knotx.engine.api.proxy.CacheableProxy;
import io.knotx.engine.api.proxy.FragmentOperation;
import io.knotx.engine.api.proxy.OperationProxyFactory;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.util.Optional;

@CacheableProxy
public class EBOperationProxyFactory implements OperationProxyFactory {

  @Override
  public String getName() {
    return "eb";
  }

  @Override
  public FragmentOperation create(String alias, JsonObject config, Optional<FragmentOperation> proxy,
      Vertx vertx) {
    String address = config.getString("address");
    DeliveryOptions deliveryOptions = new DeliveryOptions(
        config.getJsonObject("deliveryOptions") == null ? new JsonObject()
            : config.getJsonObject("deliveryOptions"));

    return EBOperationProxy.create(vertx, address, deliveryOptions);
  }

}
