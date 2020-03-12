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

import io.knotx.fragments.handler.action.http.options.EndpointOptions;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

class BodyComposer {

  private final String body;
  private final JsonObject bodyJson;
  private final boolean interpolateBody;

  BodyComposer(EndpointOptions endpointOptions) {
    this.body = endpointOptions.getBody();
    this.bodyJson = endpointOptions.getBodyJson();
    this.interpolateBody = endpointOptions.isInterpolateBody();
    ensureAtMostOneBodyConfiguration();
  }

  private void ensureAtMostOneBodyConfiguration() {
    if (StringUtils.isNotBlank(body) && !bodyJson.isEmpty()) {
      throw new IllegalArgumentException(
          "Ambiguous body parameter - both body and bodyJson specified.");
    }
  }

  String getBody(EndpointPlaceholdersResolver resolver) {
    if (interpolateBody) {
      return getInterpolatedBody(resolver);
    } else {
      return getPlainBody();
    }
  }

  boolean shouldUseJsonBody() {
    return !bodyJson.isEmpty();
  }

  private String getInterpolatedBody(EndpointPlaceholdersResolver resolver) {
    if (shouldUseJsonBody()) {
      return resolver.resolve(bodyJson).toString();
    } else {
      return resolver.resolve(body);
    }
  }

  private String getPlainBody() {
    if (shouldUseJsonBody()) {
      return bodyJson.toString();
    } else {
      return body;
    }
  }

}
