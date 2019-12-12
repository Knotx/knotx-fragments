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
package io.knotx.fragments.handler.action.cb;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class CircuitBreakerActionFactoryOptions {

  /**
   * Default value of the fallback on failure property.
   */
  private static final boolean DEFAULT_FALLBACK_ON_FAILURE = true;

  private static final String DEFAULT_NODE_LOG_LEVEL = "error";

  private static final CircuitBreakerOptions DEFAULT_CIRCUIT_BREAKER_OPTIONS = initCircuitBreakerOptions(
      new CircuitBreakerOptions());

  private String circuitBreakerName;

  private CircuitBreakerOptions circuitBreakerOptions = DEFAULT_CIRCUIT_BREAKER_OPTIONS;

  private String logLevel = DEFAULT_NODE_LOG_LEVEL;

  /**
   * Creates a new instance of {@link CircuitBreakerActionFactoryOptions} using the default values.
   */
  public CircuitBreakerActionFactoryOptions() {
    // Empty constructor
  }

  /**
   * Creates a new instance of {@link CircuitBreakerActionFactoryOptions} from the given json
   * object.
   *
   * @param json the json object
   */
  public CircuitBreakerActionFactoryOptions(JsonObject json) {
    CircuitBreakerActionFactoryOptionsConverter.fromJson(json, this);
    this.circuitBreakerOptions = initCircuitBreakerOptions(circuitBreakerOptions);
  }

  private static CircuitBreakerOptions initCircuitBreakerOptions(CircuitBreakerOptions options) {
    CircuitBreakerOptions newOptions = new CircuitBreakerOptions(options);
    newOptions.setFallbackOnFailure(DEFAULT_FALLBACK_ON_FAILURE);
    return newOptions;
  }

  /**
   * @return a json object representing the current configuration.
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CircuitBreakerActionFactoryOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * @return the unique circuit breaker name.
   */
  public String getCircuitBreakerName() {
    return StringUtils.isNoneBlank(circuitBreakerName) ? circuitBreakerName
        : RandomStringUtils.random(10, true, false);
  }

  /**
   * Sets the circuit breaker name.
   *
   * @param circuitBreakerName the circuit breaker name.
   * @return the current {@link CircuitBreakerActionFactoryOptions} instance
   */
  public CircuitBreakerActionFactoryOptions setCircuitBreakerName(String circuitBreakerName) {
    this.circuitBreakerName = circuitBreakerName;
    return this;
  }

  /**
   * @return the circuit breaker configuration options
   */
  public CircuitBreakerOptions getCircuitBreakerOptions() {
    return circuitBreakerOptions;
  }

  /**
   * Sets the circuit breaker configuration options. Note that Knot.x enforce the fallback on error
   * strategy.
   *
   * @param circuitBreakerOptions the circuit breaker name.
   * @return the current {@link CircuitBreakerActionFactoryOptions} instance
   */
  public CircuitBreakerActionFactoryOptions setCircuitBreakerOptions(
      CircuitBreakerOptions circuitBreakerOptions) {
    this.circuitBreakerOptions = circuitBreakerOptions;
    return this;
  }

  /**
   * @return the action node log level
   */
  public String getLogLevel() {
    return logLevel;
  }

  /**
   * Sets the action node log level.
   *
   * @param logLevel the log level.
   * @return the current {@link CircuitBreakerActionFactoryOptions} instance
   */
  public CircuitBreakerActionFactoryOptions setLogLevel(String logLevel) {
    this.logLevel = logLevel;
    return this;
  }
}
