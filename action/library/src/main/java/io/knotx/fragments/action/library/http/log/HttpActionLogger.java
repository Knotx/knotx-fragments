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

import io.knotx.commons.json.MultiMapTransformer;
import io.knotx.fragments.action.library.http.options.EndpointOptions;
import io.knotx.fragments.action.library.http.request.EndpointRequest;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import org.apache.commons.lang3.StringUtils;

public class HttpActionLogger {

  private HttpActionNodeLogger httpActionNodeLogger;
  private EndpointOptions endpointOptions;
  private String httpMethod;

  private EndpointRequest endpointRequest;
  private HttpResponseData httpResponseData;
  private Buffer httpResponseBody;

  private HttpActionLogger(HttpActionNodeLogger httpActionNodeLogger,
      EndpointOptions endpointOptions, String httpMethod) {
    this.httpActionNodeLogger = httpActionNodeLogger;
    this.endpointOptions = endpointOptions;
    this.httpMethod = httpMethod;
  }

  public static HttpActionLogger create(String actionAlias, ActionLogLevel logLevel,
      EndpointOptions endpointOptions, String httpMethod) {
    return new HttpActionLogger(HttpActionNodeLogger.create(actionAlias, logLevel),
        endpointOptions, httpMethod);
  }

  public JsonObject getJsonNodeLog() {
    return httpActionNodeLogger.getJsonNodeLog();
  }

  public void onRequestCreation(EndpointRequest endpointRequest) {
    this.endpointRequest = endpointRequest;
    logRequest(ActionLogLevel.INFO);
  }

  public void onRequestSucceeded(HttpResponse<Buffer> response) {
    this.httpResponseData = HttpResponseData.from(response);
    this.httpResponseBody = response.body();
    logResponse(ActionLogLevel.INFO);
    logResponseOnLogbackLogger();
  }

  private void logResponseOnLogbackLogger() {
    HttpActionLogbackLogger.logResponseOnLogbackLogger(httpResponseData, httpMethod, getRequestPath());
  }

  public void onRequestFailed(Throwable throwable) {
    logRequest(ActionLogLevel.ERROR);
    logError(throwable);
  }

  public void onResponseCodeUnsuccessful(Throwable throwable) {
    logRequest(ActionLogLevel.ERROR);
    if (httpResponseData != null) {
      logResponse(ActionLogLevel.ERROR);
    }
    logError(throwable);
  }

  public void onResponseProcessingFailed(Throwable throwable) {
    logRequest(ActionLogLevel.ERROR);
    logResponse(ActionLogLevel.ERROR);
    logError(throwable);
  }

  public void onResponseCodeSuccessful() {
    logResponseBody();
  }

  private void logResponseBody() {
    httpActionNodeLogger.logResponseBody(httpResponseBody != null ? httpResponseBody.toString() : StringUtils.EMPTY);
  }

  public void onDifferentError(Throwable throwable) {
    if (endpointRequest != null) {
      logRequest(ActionLogLevel.ERROR);
    }
    if (httpResponseData != null) {
      logResponse(ActionLogLevel.ERROR);
    }
    logError(throwable);
  }

  private void logRequest(ActionLogLevel logLevel) {
    httpActionNodeLogger.logRequest(logLevel, getRequestData());
  }

  private void logResponse(ActionLogLevel logLevel) {
    httpActionNodeLogger.logResponse(logLevel, getResponseData());
  }

  private void logError(Throwable throwable) {
    httpActionNodeLogger.logError(throwable);
  }

  private JsonObject getRequestData() {
    return new JsonObject().put("path", endpointRequest.getPath())
        .put("requestHeaders", MultiMapTransformer.toJson(endpointRequest.getHeaders()))
        .put("requestBody", endpointRequest.getBody());
  }

  private JsonObject getResponseData() {
    return httpResponseData.toJson().put("httpMethod", httpMethod)
        .put("requestPath", getRequestPath());
  }

  private String getRequestPath() {
    return endpointOptions.getDomain() + ":" + endpointOptions.getPort() + endpointRequest
        .getPath();
  }

}
