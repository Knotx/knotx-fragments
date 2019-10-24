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

import io.knotx.fragments.task.ConfigurationTaskProviderFactory;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject(generateConverter = true)
public class TaskOptions {

  private String factory;
  private JsonObject config;
  private GraphNodeOptions graph;

  public TaskOptions(JsonObject json) {
    init();
    TaskOptionsConverter.fromJson(json, this);
    if (graph == null) {
      graph = new GraphNodeOptions(json);
    }
  }

  private void init() {
    factory = ConfigurationTaskProviderFactory.NAME;
    config = new JsonObject();
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    TaskOptionsConverter.toJson(this, result);
    return result;
  }


  public String getFactory() {
    return factory;
  }

  public void setFactory(String factory) {
    this.factory = factory;
  }

  public JsonObject getConfig() {
    return config;
  }

  public void setConfig(JsonObject config) {
    this.config = config;
  }

  public GraphNodeOptions getGraph() {
    return graph;
  }

  public void setGraph(GraphNodeOptions graph) {
    this.graph = graph;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskOptions that = (TaskOptions) o;
    return Objects.equals(factory, that.factory) &&
        Objects.equals(config, that.config) &&
        Objects.equals(graph, that.graph);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, config, graph);
  }

  @Override
  public String toString() {
    return "TaskOptions{" +
        "factory='" + factory + '\'' +
        ", config=" + config +
        ", graph=" + graph +
        '}';
  }
}
