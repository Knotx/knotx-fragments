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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.handler;

import io.knotx.engine.core.KnotEngine;
import io.knotx.engine.core.impl.KnotEngineFactory;
import io.knotx.server.api.context.RequestContext;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;

public class KnotEngineHandler implements Handler<RoutingContext> {

  private FragmentEventProducer eventProducer;
  private KnotEngine engine;

  KnotEngineHandler(Vertx vertx, JsonObject config) {
    KnotEngineHandlerOptions options = new KnotEngineHandlerOptions(config);
    eventProducer = new FragmentEventProducer(options.getFlows());
    engine = KnotEngineFactory.get(vertx, options.getDeliveryOptions());
  }

  @Override
  public void handle(RoutingContext routingContext) {
    RequestContext requestContext = routingContext.get(RequestContext.KEY);
    engine.execute(eventProducer.get(requestContext.getRequestEvent().getFragments()),
        requestContext.getRequestEvent().getClientRequest())
        .subscribe(
            fragmentEvents -> {
              // TODO implement logic here
            },
            onError -> {
              // TODO handle error
            }
        );

  }
}
