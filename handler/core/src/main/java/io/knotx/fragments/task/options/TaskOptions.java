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

import io.knotx.fragments.task.factory.DefaultTaskFactory;
import io.knotx.fragments.task.TaskFactory;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Task options.
 */
@DataObject(generateConverter = true)
public class TaskOptions {

  private String factory;
  private JsonObject config;
  private GraphNodeOptions graph;

  public TaskOptions() {
  }

  public TaskOptions(JsonObject json) {
    init();
    TaskOptionsConverter.fromJson(json, this);
    if (graph == null) {
      graph = new GraphNodeOptions(json);
    }
  }

  private void init() {
    factory = DefaultTaskFactory.NAME;
    config = new JsonObject();
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    TaskOptionsConverter.toJson(this, result);
    return result;
  }

  /**
   * Gets {@link TaskFactory} name
   *
   * @return task provider factory name
   */
  public String getFactory() {
    return factory;
  }

  /**
   * Sets task provider factory name
   *
   * @param factory - task provider factory name
   * @return reference to this, so the API can be used fluently
   */
  public TaskOptions setFactory(String factory) {
    this.factory = factory;
    return this;
  }

  public JsonObject getConfig() {
    return config;
  }

  /**
   * Gets task provider factory configuration.
   *
   * @param config task provider factory configuration
   * @return reference to this, so the API can be used fluently
   */
  public TaskOptions setConfig(JsonObject config) {
    this.config = config;
    return this;
  }

  /**
   * Gets task graph of executable nodes.
   *
   * @return graph of nodes
   */
  public GraphNodeOptions getGraph() {
    return graph;
  }

  /**
   * Sets task graph.
   *
   * @param graph - graph of nodes
   * @return reference to this, so the API can be used fluently
   */
  public TaskOptions setGraph(GraphNodeOptions graph) {
    this.graph = graph;
    return this;
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
