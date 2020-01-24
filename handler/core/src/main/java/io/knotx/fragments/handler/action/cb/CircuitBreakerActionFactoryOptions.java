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

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.ERROR;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class CircuitBreakerActionFactoryOptions {

  /**
   * Default value of the fallback on failure property.
   */
  private static final boolean DEFAULT_FALLBACK_ON_FAILURE = true;

  private String circuitBreakerName;

  private CircuitBreakerOptions circuitBreakerOptions = new CircuitBreakerOptions();

  private Set<String> errorTransitions = new HashSet<>();

  private String logLevel = ERROR.getLevel();

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
        : UUID.randomUUID().toString();
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
    CircuitBreakerOptions result = new CircuitBreakerOptions(circuitBreakerOptions);
    result.setFallbackOnFailure(DEFAULT_FALLBACK_ON_FAILURE);
    return result;
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
   * @return transitions that mean error
   */
  public Set<String> getErrorTransitions() {
    Set<String> result = new HashSet<>(errorTransitions);
    result.add(ERROR_TRANSITION);
    return result;
  }

  /**
   * Sets error transitions.
   *
   * @param errorTransitions transitions that mean error
   * @return the current {@link CircuitBreakerActionFactoryOptions} instance
   */
  public CircuitBreakerActionFactoryOptions setErrorTransitions(Set<String> errorTransitions) {
    this.errorTransitions = errorTransitions;
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

  @Override
  public String toString() {
    return "CircuitBreakerActionFactoryOptions{" +
        "circuitBreakerName='" + circuitBreakerName + '\'' +
        ", circuitBreakerOptions=" + circuitBreakerOptions +
        ", errorTransitions=" + errorTransitions +
        ", logLevel='" + logLevel + '\'' +
        '}';
  }
}
