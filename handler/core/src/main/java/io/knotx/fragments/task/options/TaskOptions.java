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

  private GraphNodeOptions graph;

  public TaskOptions() {
  }

  public TaskOptions(JsonObject json) {
    TaskOptionsConverter.fromJson(json, this);
    if (graph == null) {
      graph = new GraphNodeOptions(json);
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    TaskOptionsConverter.toJson(this, result);
    return result;
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
    return Objects.equals(graph, that.graph);
  }

  @Override
  public int hashCode() {
    return Objects.hash(graph);
  }

  @Override
  public String toString() {
    return "TaskOptions{" +
        "graph=" + graph +
        '}';
  }
}
