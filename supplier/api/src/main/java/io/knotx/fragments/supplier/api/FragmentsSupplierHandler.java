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
package io.knotx.fragments.supplier.api;

import io.knotx.fragment.Fragment;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.handler.DefaultRequestContextEngine;
import io.knotx.server.api.handler.RequestContextEngine;
import io.knotx.server.api.handler.RequestEventHandlerResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;

public class FragmentsSupplierHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentsSupplierHandler.class);

  private final RequestContextEngine engine;

  FragmentsSupplier supplier;

  public FragmentsSupplierHandler(FragmentsSupplier supplier) {
    this.supplier = supplier;
    engine = new DefaultRequestContextEngine(getClass().getSimpleName());
  }

  @Override
  public void handle(RoutingContext context) {
    RequestContext requestContext = context.get(RequestContext.KEY);
    try {
      RequestEventHandlerResult result;
      try {
        List<Fragment> fragments = supplier.getFragments(requestContext);
        context.put("fragments", ObjectUtils.defaultIfNull(fragments, Collections.emptyList()));
        result = RequestEventHandlerResult.success(requestContext.getRequestEvent());
      } catch (FragmentsProvisionException e) {
        LOGGER.error(e.getMessage());
        result = RequestEventHandlerResult.fail(e.getMessage());
      }
      engine.processAndSaveResult(result, context, requestContext);
    } catch (Exception e) {
      engine.handleFatal(context, requestContext, e);
    }
  }

}
