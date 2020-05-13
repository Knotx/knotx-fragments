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
package io.knotx.fragments.action.library.http.log;

import static io.netty.handler.codec.http.HttpStatusClass.CLIENT_ERROR;
import static io.netty.handler.codec.http.HttpStatusClass.SERVER_ERROR;

import io.knotx.commons.json.MultiMapTransformer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

final class HttpActionLogbackLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpActionLogbackLogger.class);

  private HttpActionLogbackLogger() {
    // utility
  }

  static void logResponseOnLogbackLogger(HttpResponseData httpResponseData,
      String httpMethod, String requestPath) {
    if (isHttpErrorResponse(httpResponseData)) {
      LOGGER.error("{} {} -> Error response {}, headers[{}]",
          getVertxLogResponseParameters(httpResponseData, httpMethod, requestPath));
    } else if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} {} -> Got response {}, headers[{}]",
          getVertxLogResponseParameters(httpResponseData, httpMethod, requestPath));
    }
  }

  private static boolean isHttpErrorResponse(HttpResponseData httpResponseData) {
    return CLIENT_ERROR.contains(Integer.parseInt(httpResponseData.getStatusCode())) || SERVER_ERROR
        .contains(Integer.parseInt(httpResponseData.getStatusCode()));
  }

  private static Object[] getVertxLogResponseParameters(HttpResponseData httpResponseData,
      String httpMethod, String requestPath) {
    return new Object[]{
        httpMethod,
        requestPath,
        httpResponseData.getStatusCode(),
        MultiMapTransformer.toJson(httpResponseData.getHeaders())
    };
  }
}
