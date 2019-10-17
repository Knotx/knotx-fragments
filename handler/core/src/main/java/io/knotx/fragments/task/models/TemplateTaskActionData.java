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
package io.knotx.fragments.task.models;

import io.vertx.core.json.JsonObject;

public class TemplateTaskActionData {

  private static final String ACTION_KEY = "action";
  private static final String NAMESPACE_KEY = "namespace";
  private static final String EXTRA_KEY = "extra";

  private String action;
  private String namespace;
  private JsonObject extra;

  TemplateTaskActionData(JsonObject json) {
    this.action = json.getString(ACTION_KEY);
    this.namespace = json.getString(NAMESPACE_KEY);
    this.extra = json.getJsonObject(EXTRA_KEY);
  }

  public String getAction() {
    return action;
  }

  public String getNamespace() {
    return namespace;
  }

  public JsonObject getExtra() {
    return extra;
  }

  @Override
  public String toString() {
    return "TemplateTaskActionData{" +
        "action='" + action + '\'' +
        ", namespace='" + namespace + '\'' +
        ", extra=" + extra +
        '}';
  }
}
