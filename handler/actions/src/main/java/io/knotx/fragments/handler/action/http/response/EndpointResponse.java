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
package io.knotx.fragments.handler.action.http.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpVersion;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;

public class EndpointResponse {

  private final HttpResponseStatus statusCode;
  private String statusMessage;
  private HttpVersion httpVersion;
  private MultiMap headers = MultiMap.caseInsensitiveMultiMap();
  private MultiMap trailers = MultiMap.caseInsensitiveMultiMap();
  private Buffer body;

  public EndpointResponse(HttpResponseStatus statusCode) {
    this.statusCode = statusCode;
  }

  public static EndpointResponse fromHttpResponse(HttpResponse<Buffer> response) {
    EndpointResponse endpointResponse = new EndpointResponse(
        HttpResponseStatus.valueOf(response.statusCode()));
    endpointResponse.body = getResponseBody(response);
    endpointResponse.headers = response.headers();
    endpointResponse.trailers = response.trailers();
    endpointResponse.statusMessage = response.statusMessage();
    endpointResponse.httpVersion = response.version();
    return endpointResponse;
  }


  HttpResponseStatus getStatusCode() {
    return statusCode;
  }

  public MultiMap getHeaders() {
    return headers;
  }

  public MultiMap getTrailers() {
    return trailers;
  }

  public Buffer getBody() {
    return body;
  }

  String getStatusMessage() {
    return statusMessage;
  }

  public HttpVersion getHttpVersion() {
    return httpVersion;
  }

  @Override
  public String toString() {
    return "EndpointResponse{" +
        "statusCode=" + statusCode +
        ", statusMessage='" + statusMessage + '\'' +
        ", httpVersion=" + httpVersion +
        ", headers=" + headers +
        ", trailers=" + trailers +
        ", body=" + body +
        '}';
  }

  private static Buffer getResponseBody(HttpResponse<Buffer> response) {
    if (response.body() != null) {
      return response.body();
    } else {
      return Buffer.buffer();
    }
  }
}
