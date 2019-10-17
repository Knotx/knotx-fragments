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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateTaskData {

  private Map<String, List<TemplateTaskActionData>> actions;

  public TemplateTaskData(JsonObject json) {
    actions = new HashMap<>();
    json.iterator().forEachRemaining(entry -> {
      JsonArray actions = (JsonArray) entry.getValue();
      List<TemplateTaskActionData> value = new ArrayList<>();
      actions.iterator().forEachRemaining(i -> value.add(transform(i)));
      this.actions.put(entry.getKey(), value);
    });
  }

  private TemplateTaskActionData transform(Object i) {
    JsonObject actionTemplateJson = (JsonObject) i;
    return new TemplateTaskActionData(actionTemplateJson);
  }

  public List<TemplateTaskActionData> get(String namespace) {
    return actions.get(namespace);
  }
}
