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
package io.knotx.fragments.handler.action.http.options;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * HTTP Action configuration
 */
@DataObject(generateConverter = true)
public class HttpActionOptions {

  private static final long DEFAULT_REQUEST_TIMEOUT = 0L;

  private String httpMethod = HttpMethod.GET.name();
  private WebClientOptions webClientOptions = new WebClientOptions();
  private EndpointOptions endpointOptions = new EndpointOptions();
  private ResponseOptions responseOptions = new ResponseOptions();
  private long requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT;
  private String logLevel;

  public HttpActionOptions() {
  }

  public HttpActionOptions(JsonObject json) {
    HttpActionOptionsConverter.fromJson(json, this);
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  /**
   * Set the {@code HttpMethod} used for performing the request.
   * At the moment only HTTP GET method is supported.
   *
   * @param httpMethod HTTP method
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  public WebClientOptions getWebClientOptions() {
    return webClientOptions;
  }

  /**
   * Set the {@code WebClientOptions} used by the HTTP client to communicate with remote http
   * endpoint. See https://vertx.io/docs/vertx-web-client/dataobjects.html#WebClientOptions for the
   * details what can be configured.
   *
   * @param webClientOptions {@link WebClientOptions} object
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setWebClientOptions(WebClientOptions webClientOptions) {
    this.webClientOptions = webClientOptions;
    return this;
  }

  public EndpointOptions getEndpointOptions() {
    return endpointOptions;
  }

  /**
   * Set the details of the remote http endpoint location.
   *
   * @param endpointOptions a {@link EndpointOptions} object
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setEndpointOptions(EndpointOptions endpointOptions) {
    this.endpointOptions = endpointOptions;
    return this;
  }

  public ResponseOptions getResponseOptions() {
    return responseOptions;
  }

  public HttpActionOptions setResponseOptions(ResponseOptions responseOptions) {
    this.responseOptions = responseOptions;
    return this;
  }

  public long getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  /**
   * Configures the amount of time in milliseconds after which if the request does not return any
   * data within, _timeout transition will be returned. Setting zero or a negative value disables
   * the timeout. By default it is set to {@code 0}.
   *
   * @param requestTimeoutMs - request timeout in milliseconds
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setRequestTimeoutMs(long requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
    return this;
  }

  public String getLogLevel() {
    return logLevel;
  }

  /**
   * Set level of action logs.
   *
   * @param logLevel alevel of action logs
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setLogLevel(String logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  @Override
  public String toString() {
    return "HttpActionOptions{" +
        "webClientOptions=" + webClientOptions +
        ", endpointOptions=" + endpointOptions +
        ", responseOptions=" + responseOptions +
        ", requestTimeoutMs=" + requestTimeoutMs +
        ", logLevel=" + logLevel +
        '}';
  }
}
