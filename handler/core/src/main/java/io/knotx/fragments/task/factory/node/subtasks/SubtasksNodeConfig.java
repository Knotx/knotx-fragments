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
package io.knotx.fragments.task.factory.node.subtasks;

import io.knotx.fragments.task.factory.GraphNodeOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * Subtask Node configuration.
 */
@DataObject(generateConverter = true)
public class SubtasksNodeConfig {

  private List<GraphNodeOptions> subtasks;

  public SubtasksNodeConfig(List<GraphNodeOptions> subtasks) {
    this.subtasks = subtasks;
  }

  public SubtasksNodeConfig(JsonObject json) {
    SubtasksNodeConfigConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SubtasksNodeConfigConverter.toJson(this, json);
    return json;
  }

  public List<GraphNodeOptions> getSubtasks() {
    return subtasks;
  }

  /**
   * The array/list of subgraphs/subtasks that can be executed in parallel.
   *
   * @param subtasks list of subgraphs
   * @return reference to this, so the API can be used fluently
   */
  public SubtasksNodeConfig setSubtasks(List<GraphNodeOptions> subtasks) {
    this.subtasks = subtasks;
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
    SubtasksNodeConfig that = (SubtasksNodeConfig) o;
    return Objects.equals(subtasks, that.subtasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subtasks);
  }

  @Override
  public String toString() {
    return "SubtasksNodeOptions{" +
        "subtasks=" + subtasks +
        '}';
  }
}
