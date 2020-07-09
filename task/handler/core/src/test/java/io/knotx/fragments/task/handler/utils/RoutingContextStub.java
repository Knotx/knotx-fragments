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
package io.knotx.fragments.task.handler.utils;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.knotx.fragments.api.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Map;
import org.mockito.Mockito;

public final class RoutingContextStub {

  public static RoutingContext create(Fragment fragment, Map<String, String> headers,
      Map<String, String> params) {
    ClientRequest clientRequest = new ClientRequest();

    MultiMap paramsMultiMap = MultiMap.caseInsensitiveMultiMap();
    paramsMultiMap.addAll(params);
    clientRequest.setParams(paramsMultiMap);

    MultiMap headersMultiMap = MultiMap.caseInsensitiveMultiMap();
    headersMultiMap.addAll(headers);
    clientRequest.setHeaders(headersMultiMap);

    RequestContext requestContext = new RequestContext(
        new RequestEvent(clientRequest, new JsonObject()));

    RoutingContext routingContext = Mockito.mock(RoutingContext.class);

    when(routingContext.get(eq(RequestContext.KEY))).thenReturn(requestContext);
    when(routingContext.get(eq("fragments"))).thenReturn(newArrayList(fragment));
    return routingContext;
  }
}
