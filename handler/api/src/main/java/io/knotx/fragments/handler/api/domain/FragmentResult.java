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
package io.knotx.fragments.handler.api.domain;

import io.knotx.fragments.api.Fragment;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Result of the {@code Action} fragment processing.
 */
@DataObject
public class FragmentResult {

  public static final String SUCCESS_TRANSITION = "_success";
  public static final String ERROR_TRANSITION = "_error";

  private static final String FRAGMENT_KEY = "fragment";
  private static final String TRANSITION_KEY = "transition";
  private static final String ACTION_LOG_KEY = "actionLog";

  private final Fragment fragment;
  private final String transition;
  private final JsonObject actionLog;

  public FragmentResult(Fragment fragment, String transition, JsonObject actionLog) {
    this.fragment = fragment;
    this.transition = transition;
    this.actionLog = actionLog;
  }
  public FragmentResult(Fragment fragment, String transition) {
    this(fragment, transition, new JsonObject());
  }

  public FragmentResult(JsonObject json) {
    this.fragment = new Fragment(json.getJsonObject(FRAGMENT_KEY));
    this.transition = json.getString(TRANSITION_KEY);
    this.actionLog = json.getJsonObject(ACTION_LOG_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_KEY, fragment.toJson())
        .put(TRANSITION_KEY, transition)
        .put(ACTION_LOG_KEY, actionLog);
  }

  /**
   * A {@code Fragment} transformed or updated during applying the {@code Action}.
   *
   * @return transformed or updated Fragment
   */
  public Fragment getFragment() {
    return fragment;
  }

  /**
   * Name of the next step in the graph that is defined as the {@code Action} output.
   *
   * @return next transition
   */
  public String getTransition() {
    if (StringUtils.isBlank(transition)) {
      return SUCCESS_TRANSITION;
    } else {
      return transition;
    }
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
        Objects.equals(transition, that.transition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragment, transition);
  }

  @Override
  public String toString() {
    return "FragmentResult{" +
        "fragment=" + fragment +
        ", transition='" + transition + '\'' +
        '}';
  }

  public JsonObject getActionLog() {
    return actionLog;
  }
}