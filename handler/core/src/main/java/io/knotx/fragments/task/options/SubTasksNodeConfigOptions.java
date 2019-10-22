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
import java.util.List;
import java.util.Objects;

@DataObject(generateConverter = true)
public class SubTasksNodeConfigOptions {

  private List<GraphOptions> subTasks;

  public SubTasksNodeConfigOptions(List<GraphOptions> subTasks) {
    this.subTasks = subTasks;
  }

  public SubTasksNodeConfigOptions(JsonObject json) {
    SubTasksNodeConfigOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SubTasksNodeConfigOptionsConverter.toJson(this, json);
    return json;
  }

  public List<GraphOptions> getSubTasks() {
    return subTasks;
  }

  /**
   * Sets list of {@link GraphOptions} that represents {@link io.knotx.fragments.engine.Task} that
   * will be executed in parallel.
   *
   * @param subTasks list of {@link GraphOptions}
   * @return reference to this, so the API can be used fluently
   */
  public SubTasksNodeConfigOptions setSubTasks(List<GraphOptions> subTasks) {
    this.subTasks = subTasks;
    return this;
  }

  public SubTasksNodeConfigOptions setSubtasks(List<GraphOptions> subtasks) {
    setSubTasks(subtasks);
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
    SubTasksNodeConfigOptions that = (SubTasksNodeConfigOptions) o;
    return Objects.equals(subTasks, that.subTasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subTasks);
  }

  @Override
  public String toString() {
    return "SubTasksNodeOptions{" +
        "subTasks=" + subTasks +
        '}';
  }
}
