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
package io.knotx.fragments.handler;

import io.knotx.fragments.task.TaskFactoryOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * Fragments Handler options model.
 */
@DataObject(generateConverter = true)
public class FragmentsHandlerOptions {

  private List<TaskFactoryOptions> taskFactories;

  public FragmentsHandlerOptions(JsonObject json) {
    FragmentsHandlerOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    FragmentsHandlerOptionsConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public List<TaskFactoryOptions> getTaskFactories() {
    return taskFactories;
  }

  /**
   * The array/list of task factory options defines factories taking part in the creation of tasks. First
   * items on the list have the highest priority.
   *
   * @param taskFactories - a list of task factory options
   */
  public void setTaskFactories(List<TaskFactoryOptions> taskFactories) {
    this.taskFactories = taskFactories;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FragmentsHandlerOptions that = (FragmentsHandlerOptions) o;
    return Objects.equals(taskFactories, that.taskFactories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskFactories);
  }

  @Override
  public String toString() {
    return "FragmentsHandlerOptions{" +
        "taskFactories=" + taskFactories +
        '}';
  }
}
