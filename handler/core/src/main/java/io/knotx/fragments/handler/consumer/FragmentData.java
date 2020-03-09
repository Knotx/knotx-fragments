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
package io.knotx.fragments.handler.consumer;

import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class FragmentData {

  private String id;
  private String type;

  public FragmentData(FragmentEvent fragmentEvent) {
    this.id = fragmentEvent.getFragment().getId();
    this.type = fragmentEvent.getFragment().getType();
  }

  public FragmentData(JsonObject jsonObject) {
    FragmentDataConverter.fromJson(jsonObject, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    FragmentDataConverter.toJson(this, json);
    return json;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }
}
