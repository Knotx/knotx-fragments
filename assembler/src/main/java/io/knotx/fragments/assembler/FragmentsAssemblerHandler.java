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
package io.knotx.fragments.assembler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.knotx.fragments.api.Fragment;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.knotx.server.api.handler.DefaultRequestContextEngine;
import io.knotx.server.api.handler.RequestContextEngine;
import io.knotx.server.api.handler.RequestEventHandlerResult;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.ext.web.RoutingContext;

class FragmentsAssemblerHandler implements Handler<RoutingContext> {

  private static final String MISSING_FRAGMENTS_PAYLOAD = "Expected 'fragments' in the routing context are missing!";

  private final RequestContextEngine engine;

  FragmentsAssemblerHandler() {
    engine = new DefaultRequestContextEngine(getClass().getSimpleName());
  }

  @Override
  public void handle(RoutingContext context) {
    RequestContext requestContext = context.get(RequestContext.KEY);
    try {
      RequestEventHandlerResult result = joinFragmentsBodies(context,
          requestContext.getRequestEvent());
      engine.processAndSaveResult(result, context, requestContext);
    } catch (Exception e) {
      engine.handleFatal(context, requestContext, e);
    }
  }

  RequestEventHandlerResult joinFragmentsBodies(RoutingContext context,
      RequestEvent requestEvent) {
    final List<Fragment> fragments = context.get("fragments");

    final String responseBody = Optional.ofNullable(fragments)
        .map(this::toResponseBody)
        .orElseThrow(() -> new IllegalStateException(MISSING_FRAGMENTS_PAYLOAD));

    return createSuccessResponse(requestEvent, responseBody);
  }

  private String toResponseBody(List<Fragment> fragments) {
    return fragments.stream()
        .map(Fragment::getBody)
        .collect(Collectors.joining());
  }

  private RequestEventHandlerResult createSuccessResponse(RequestEvent inputContext,
      String responseBody) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    headers.add(HttpHeaders.CONTENT_LENGTH.toString().toLowerCase(),
        Integer.toString(responseBody.length()));

    return RequestEventHandlerResult.success(inputContext)
        .withBody(Buffer.buffer(responseBody))
        .withHeaders(headers);
  }

}
