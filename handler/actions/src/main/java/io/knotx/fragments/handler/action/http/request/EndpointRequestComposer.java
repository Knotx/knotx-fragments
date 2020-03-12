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
package io.knotx.fragments.handler.action.http.request;

import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.handler.action.http.options.EndpointOptions;
import io.vertx.reactivex.core.MultiMap;

public class EndpointRequestComposer {

  private final EndpointOptions endpointOptions;
  private final HeadersComposer headersComposer;
  private final BodyComposer bodyComposer;

  public EndpointRequestComposer(EndpointOptions endpointOptions) {
    this.endpointOptions = endpointOptions;
    this.headersComposer = new HeadersComposer(endpointOptions);
    this.bodyComposer = new BodyComposer(endpointOptions);
  }

  public EndpointRequest createEndpointRequest(FragmentContext context) {
    EndpointPlaceholdersResolver resolver = new EndpointPlaceholdersResolver(context);
    String path = getPath(resolver);
    MultiMap requestHeaders = headersComposer.getRequestHeaders(context.getClientRequest());
    String body = bodyComposer.getBody(resolver);
    setContentTypeIfApplicable(requestHeaders);
    return new EndpointRequest(path, requestHeaders, body);
  }

  private void setContentTypeIfApplicable(MultiMap requestHeaders) {
    if(bodyComposer.shouldUseJsonBody()) {
      headersComposer.setJsonContentTypeIfEmpty(requestHeaders);
    }
  }

  private String getPath(EndpointPlaceholdersResolver resolver) {
    if (endpointOptions.isInterpolatePath()) {
      return resolver.resolveAndEncode(endpointOptions.getPath());
    } else {
      return endpointOptions.getPath();
    }
  }

}
