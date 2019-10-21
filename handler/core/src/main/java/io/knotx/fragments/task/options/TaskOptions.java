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
package io.knotx.fragments.task.options;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = false)
public class TaskOptions {

  private static final String FACTORY_KEY = "factory";
  private static final String GRAPH_KEY = "graph";

  private TaskProviderOptions factory;
  private GraphOptions graph;

  public TaskOptions(JsonObject json) {
    if (json.containsKey(FACTORY_KEY)) {
      factory = new TaskProviderOptions(json.getJsonObject(FACTORY_KEY));
      graph = new GraphOptions(json.getJsonObject(GRAPH_KEY));
    } else {
      factory = new TaskProviderOptions("default", new JsonObject());
      graph = new GraphOptions(json);
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    result.put(FACTORY_KEY, factory);
    result.put(GRAPH_KEY, graph);
    return result;
  }

  public TaskProviderOptions getFactory() {
    return factory;
  }

  public GraphOptions getGraph() {
    return graph;
  }

  @Override
  public String toString() {
    return "TaskOptions{" +
        "factory=" + factory +
        ", graph=" + graph +
        '}';
  }
}
