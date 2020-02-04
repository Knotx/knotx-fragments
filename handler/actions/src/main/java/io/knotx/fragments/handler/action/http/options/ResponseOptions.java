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
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.Set;

@DataObject(generateConverter = true, publicConverter = false)
public class ResponseOptions {

  private Set<String> predicates;
  private boolean forceJson;

  public ResponseOptions() {
    this.predicates = new HashSet<>();
  }

  public ResponseOptions(ResponseOptions other) {
    this.predicates = new HashSet<>(other.predicates);
    this.forceJson = other.forceJson;
  }

  public ResponseOptions(JsonObject json) {
    this();
    ResponseOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ResponseOptionsConverter.toJson(this, json);
    return json;
  }

  public Set<String> getPredicates() {
    return predicates;
  }

  /**
   * Sets Vert.x response predicates
   *
   * @param predicates - Vert.x response predicates
   * @return a reference to this, so the API can be used fluently
   */
  public ResponseOptions setPredicates(Set<String> predicates) {
    this.predicates = predicates;
    return this;
  }

  public boolean isForceJson() {
    return forceJson;
  }

  /**
   * Sets forceJson - it determines if response body should be parsed as json
   *
   * @param forceJson - determines if response body should be parsed as json or not
   * @return a reference to this, so the API can be used fluently
   */
  public ResponseOptions setForceJson(boolean forceJson) {
    this.forceJson = forceJson;
    return this;
  }

  @Override
  public String toString() {
    return "ResponseOptions{" +
        "predicates=" + predicates +
        ", forceJson=" + forceJson +
        '}';
  }
}
