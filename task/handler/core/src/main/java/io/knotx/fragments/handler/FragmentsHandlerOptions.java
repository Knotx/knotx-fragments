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
package io.knotx.fragments.handler;

import io.knotx.fragments.handler.spi.FactoryOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;

/**
 * Fragments Handler options model.
 */
@DataObject(generateConverter = true)
public class FragmentsHandlerOptions {

  private List<FactoryOptions> taskFactories = Collections.emptyList();

  private List<FactoryOptions> consumerFactories = Collections.emptyList();

  private String allowInvalidFragmentsHeader;

  private String allowInvalidFragmentsParam;

  public FragmentsHandlerOptions(JsonObject json) {
    FragmentsHandlerOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    FragmentsHandlerOptionsConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public List<FactoryOptions> getTaskFactories() {
    return taskFactories;
  }

  public String getAllowInvalidFragmentsHeader() {
    return allowInvalidFragmentsHeader == null ? "" : allowInvalidFragmentsHeader;
  }

  /**
   * Name of the http header that, when set to "true" in the request, will force Fragments Handler to succeed even if some fragments failed.
   * This is mainly so that the data can be inspected in debug mode.
   *
   * @param allowInvalidFragmentsHeader name of the http header
   * @return reference to this, so the API can be used fluently
   */
  public FragmentsHandlerOptions setAllowInvalidFragmentsHeader(String allowInvalidFragmentsHeader) {
    this.allowInvalidFragmentsHeader = allowInvalidFragmentsHeader;
    return this;
  }

  public String getAllowInvalidFragmentsParam() {
    return allowInvalidFragmentsParam == null ? "" : allowInvalidFragmentsParam;
  }

  /**
   * Name of the query parameter that, when set to "true" in the request, will force Fragments Handler to succeed even if some fragments failed.
   * This is mainly so that the data can be inspected in debug mode.
   *
   * @param allowInvalidFragmentsParam name of the query parameter
   * @return reference to this, so the API can be used fluently
   */
  public FragmentsHandlerOptions setAllowInvalidFragmentsParam(String allowInvalidFragmentsParam) {
    this.allowInvalidFragmentsParam = allowInvalidFragmentsParam;
    return this;
  }

  /**
   * The array/list of task factory options defines factories taking part in the creation of tasks.
   * First items on the list have the highest priority.
   *
   * @param taskFactories - a list of task factory options
   * @return reference to this, so the API can be used fluently
   */
  public FragmentsHandlerOptions setTaskFactories(List<FactoryOptions> taskFactories) {
    this.taskFactories = taskFactories;
    return this;
  }

  public List<FactoryOptions> getConsumerFactories() {
    return consumerFactories;
  }

  /**
   * The array/list of consumer factory options.
   *
   * @param consumerFactories - a list of factory options
   * @return reference to this, so the API can be used fluently
   */
  public FragmentsHandlerOptions setConsumerFactories(List<FactoryOptions> consumerFactories) {
    this.consumerFactories = consumerFactories;
    return this;
  }

  @Override
  public String toString() {
    return "FragmentsHandlerOptions [" + toJson() + ']';
  }
}
