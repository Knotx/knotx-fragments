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
package io.knotx.fragments.handler.options;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = false)
public class TaskOptions {

  private static final String BUILDER_KEY = "builder";
  private static final String CONFIG_KEY = "config";

  private TaskBuilderOptions builder;
  private NodeOptions config;

  public TaskOptions(JsonObject json) {
    if (json.containsKey(BUILDER_KEY)) {
      builder = new TaskBuilderOptions(json.getJsonObject(BUILDER_KEY));
      config = new NodeOptions(json.getJsonObject(CONFIG_KEY));
    } else {
      builder = new TaskBuilderOptions("default", new JsonObject());
      config = new NodeOptions(json);
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    result.put(BUILDER_KEY, builder);
    result.put(CONFIG_KEY, config);
    return result;
  }

  public TaskBuilderOptions getBuilder() {
    return builder;
  }

  public NodeOptions getConfig() {
    return config;
  }

  @Override
  public String toString() {
    return "TaskOptions{" +
        "builder=" + builder +
        ", config=" + config +
        '}';
  }
}
