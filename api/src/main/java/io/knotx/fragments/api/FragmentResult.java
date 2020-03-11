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
package io.knotx.fragments.api;

import io.vertx.codegen.annotations.DataObject;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import io.vertx.core.json.JsonObject;

/**
 * Result of the {@link FragmentOperation}.
 */
@DataObject
public class FragmentResult {

  public static final String SUCCESS_TRANSITION = "_success";
  public static final String ERROR_TRANSITION = "_error";

  private static final String FRAGMENT_KEY = "fragment";
  private static final String TRANSITION_KEY = "transition";
  private static final String LOG_KEY = "log";

  private final Fragment fragment;
  private final String transition;
  private final JsonObject log;

  public FragmentResult(Fragment fragment, String transition, JsonObject log) {
    this.fragment = fragment;
    this.transition = transition;
    this.log = log;
  }

  public FragmentResult(Fragment fragment, String transition) {
    this(fragment, transition, null);
  }

  public FragmentResult(JsonObject json) {
    this.fragment = new Fragment(json.getJsonObject(FRAGMENT_KEY));
    this.transition = json.getString(TRANSITION_KEY);
    this.log = json.getJsonObject(LOG_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_KEY, fragment.toJson())
        .put(TRANSITION_KEY, transition)
        .put(LOG_KEY, log);
  }

  /**
   * A {@code Fragment} transformed or updated during applying the {@link FragmentOperation}.
   *
   * @return transformed or updated Fragment
   */
  public Fragment getFragment() {
    return fragment;
  }

  /**
   * A text value state of {@link FragmentOperation} that determines next steps in business logic.
   *
   * @return a state of {@link FragmentOperation}
   */
  public String getTransition() {
    if (StringUtils.isBlank(transition)) {
      return SUCCESS_TRANSITION;
    } else {
      return transition;
    }
  }

  /**
   * Log data produced by {@link FragmentOperation}. It is a JSON-based value specific to the
   * operation.
   *
   * @return operation log
   */
  public JsonObject getLog() {
    return log;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FragmentResult that = (FragmentResult) o;
    return Objects.equals(fragment, that.fragment) &&
        Objects.equals(transition, that.transition) &&
        Objects.equals(log, that.log);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragment, transition, log);
  }

  @Override
  public String toString() {
    return "FragmentResult{" +
        "fragment=" + fragment +
        ", transition='" + transition + '\'' +
        ", log=" + log +
        '}';
  }
}