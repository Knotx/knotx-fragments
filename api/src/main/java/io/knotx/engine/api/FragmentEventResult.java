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
package io.knotx.engine.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class FragmentEventResult {

  private static final String FRAGMENT_EVENT = "fragmentEvent";
  private static final String TRANSITION = "transition";

  private FragmentEvent fragmentEvent;
  private String transition;

  public FragmentEventResult(FragmentEvent fragmentEvent, String transition) {
    this.fragmentEvent = fragmentEvent;
    this.transition = transition;
  }

  public FragmentEventResult(JsonObject json) {
    this.fragmentEvent = new FragmentEvent(json.getJsonObject(FRAGMENT_EVENT));
    this.transition = json.getString(TRANSITION);
  }

  public JsonObject toJson() {
    return new JsonObject().put(FRAGMENT_EVENT, fragmentEvent.toJson())
        .put(TRANSITION, transition);
  }

  public FragmentEvent getFragmentEvent() {
    return fragmentEvent;
  }

  public String getTransition() {
    return transition;
  }

  @Override
  public String toString() {
    return "FragmentEventResult{" +
        "fragmentEvent=" + fragmentEvent +
        ", transition='" + transition + '\'' +
        '}';
  }
}
