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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Describes a physical details of HTTP service endpoint that Action will connect to.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class EndpointOptions {

  private String path;
  private String domain;
  private int port;
  private Set<String> allowedRequestHeaders;
  private JsonObject additionalHeaders;
  private List<Pattern> allowedRequestHeadersPatterns;
  //ToDo: private Set<StatusCode> successStatusCodes;

  public EndpointOptions() {
    //empty default constructor
  }

  public EndpointOptions(EndpointOptions other) {
    this.path = other.path;
    this.domain = other.domain;
    this.port = other.port;
    this.allowedRequestHeaders = new HashSet<>(other.allowedRequestHeaders);
    this.allowedRequestHeadersPatterns = new ArrayList<>(other.allowedRequestHeadersPatterns);
    this.additionalHeaders = other.additionalHeaders.copy();
  }

  public EndpointOptions(JsonObject json) {
    this();
    EndpointOptionsConverter.fromJson(json, this);
    if (allowedRequestHeaders != null) {
      allowedRequestHeadersPatterns = allowedRequestHeaders.stream()
          .map(Pattern::compile).collect(Collectors.toList());
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    EndpointOptionsConverter.toJson(this, json);
    return json;
  }

  public String getPath() {
    return path;
  }

  /**
   * Sets the request path to the endpoint.
   *
   * @param path an endpoint request path.
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * @return a domain of the external service
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Sets the {@code domain} of the external service
   *
   * @param domain - domain of the external service
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * @return HTTP port of the external service
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the HTTP {@code port} the external service
   *
   * @param port - HTTP port
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return Set of allowed request headers that should be passed-through to the service
   */
  public Set<String> getAllowedRequestHeaders() {
    return allowedRequestHeaders;
  }

  /**
   * Sets the allowed requests headers that should be send to the service. The selected headers from
   * the original client HTTP request are being send.
   *
   * @param allowedRequestHeaders set of Strings with header names
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setAllowedRequestHeaders(Set<String> allowedRequestHeaders) {
    this.allowedRequestHeaders = allowedRequestHeaders;
    allowedRequestHeadersPatterns = allowedRequestHeaders.stream()
        .map(Pattern::compile).collect(Collectors.toList());
    return this;
  }

  /**
   * @return a Json Object with additional headers and it's values
   */
  public JsonObject getAdditionalHeaders() {
    return additionalHeaders;
  }

  /**
   * Sets the additional request headers (and values) to be send in each request
   *
   * @param additionalHeaders - JSON Object specifying additional header
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setAdditionalHeaders(JsonObject additionalHeaders) {
    this.additionalHeaders = additionalHeaders;
    return this;
  }

  @GenIgnore
  public List<Pattern> getAllowedRequestHeadersPatterns() {
    return allowedRequestHeadersPatterns;
  }

  @GenIgnore
  public EndpointOptions setAllowedRequestHeaderPatterns(
      List<Pattern> allowedRequestHeaderPatterns) {
    this.allowedRequestHeadersPatterns = allowedRequestHeaderPatterns;
    return this;
  }
}
