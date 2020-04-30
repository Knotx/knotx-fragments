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
package io.knotx.fragments.handler;

import io.knotx.server.api.handler.RoutingHandlerFactory;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;

public class FragmentsHandlerFactory implements RoutingHandlerFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentsHandler.class);

  @Override
  public String getName() {
    return "fragmentsHandler";
  }

  @Override
  public Handler<RoutingContext> create(Vertx vertx, JsonObject config) {
    LOGGER.info("Creating Handler [{}] with options [{}].", getName(), config);
    FragmentsHandler fragmentsHandler = new FragmentsHandler(vertx, config);
    LOGGER.info("Handler [{}] created.", getName(), config );
    return fragmentsHandler;
  }
}
