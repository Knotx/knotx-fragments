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
package io.knotx.fragments.handler.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject
public class ActionExtraOptions {

  public static final String EXTRA_OPTIONS_KEY = "_extra";

  private JsonObject extra;

  public ActionExtraOptions() {
    extra = new JsonObject();
  }

  public ActionExtraOptions(JsonObject json) {
    this.extra = json.getJsonObject(EXTRA_OPTIONS_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject().put(EXTRA_OPTIONS_KEY, extra);
  }

  public JsonObject getExtra() {
    return extra;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionExtraOptions that = (ActionExtraOptions) o;
    return Objects.equals(extra, that.extra);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extra);
  }

  @Override
  public String toString() {
    return "ActionExtraOptions{" +
        "extra=" + extra +
        '}';
  }
}
