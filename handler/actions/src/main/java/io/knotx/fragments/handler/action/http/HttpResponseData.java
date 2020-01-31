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
package io.knotx.fragments.handler.action.http;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import java.util.List;
import java.util.Map.Entry;

public class HttpResponseData {

  private static final String HTTP_VERSION_KEY = "httpVersion";
  private static final String STATUS_CODE_KEY = "statusCode";
  private static final String STATUS_MESSAGE_KEY = "statusMessage";
  private static final String HEADERS_KEY = "headers";
  private static final String TRAILERS_KEY = "trailers";

  private final String httpVersion;
  private final String statusCode;
  private final String statusMessage;
  private final MultiMap headers;
  private final MultiMap trailers;

  private HttpResponseData(String httpVersion, String statusCode, String statusMessage,
      MultiMap headers, MultiMap trailers) {
    this.httpVersion = httpVersion;
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.headers = headers;
    this.trailers = trailers;
  }

  public static HttpResponseData from(HttpResponse<Buffer> response) {
    return new HttpResponseData(
        String.valueOf(response.version()),
        String.valueOf(response.statusCode()),
        response.statusMessage(),
        response.headers(),
        response.trailers()
    );
  }

  public static HttpResponseData from(EndpointResponse endpointResponse) {
    return new HttpResponseData(
        String.valueOf(endpointResponse.getHttpVersion()),
        String.valueOf(endpointResponse.getStatusCode()),
        endpointResponse.getStatusMessage(),
        endpointResponse.getHeaders(),
        endpointResponse.getTrailers()
    );
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(HTTP_VERSION_KEY, httpVersion)
        .put(STATUS_CODE_KEY, statusCode)
        .put(STATUS_MESSAGE_KEY, statusMessage)
        .put(HEADERS_KEY, multiMapToJson(headers.entries()))
        .put(TRAILERS_KEY, multiMapToJson(trailers.entries()));
  }

  public String getHttpVersion() {
    return httpVersion;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public MultiMap getHeaders() {
    return headers;
  }

  public MultiMap getTrailers() {
    return trailers;
  }

  private static JsonObject multiMapToJson(List<Entry<String, String>> entries) {
    JsonObject json = new JsonObject();
    entries.forEach(e -> json.put(e.getKey(), e.getValue()));
    return json;
  }

  @Override
  public String toString() {
    return "HttpResponseData{" +
        "httpVersion='" + httpVersion + '\'' +
        ", statusCode='" + statusCode + '\'' +
        ", statusMessage='" + statusMessage + '\'' +
        ", headers=" + headers +
        ", trailers=" + trailers +
        '}';
  }
}
