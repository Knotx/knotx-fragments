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

import io.knotx.fragments.handler.action.ActionOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Objects;

@DataObject(generateConverter = true)
public class FragmentsHandlerOptions {

  public static final String DEFAULT_TASK_KEY = "data-knotx-task";

  private String taskKey;

  private Map<String, NodeOptions> tasks;

  private Map<String, ActionOptions> actions;

  public FragmentsHandlerOptions(Map<String, NodeOptions> tasks) {
    init();
    this.tasks = tasks;
  }

  public FragmentsHandlerOptions(JsonObject json) {
    init();
    FragmentsHandlerOptionsConverter.fromJson(json, this);
  }

  private void init() {
    this.taskKey = DEFAULT_TASK_KEY;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    FragmentsHandlerOptionsConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public String getTaskKey() {
    return taskKey;
  }

  public FragmentsHandlerOptions setTaskKey(String taskKey) {
    this.taskKey = taskKey;
    return this;
  }

  public Map<String, NodeOptions> getTasks() {
    return tasks;
  }

  /**
   * Sets {@code Task} list, which are named, directed graphs of {@code Actions}.
   *
   * @param tasks list of defined {@code Tasks}.
   * @return reference to this, so the API can be used fluently
   */
  public FragmentsHandlerOptions setTasks(Map<String, NodeOptions> tasks) {
    this.tasks = tasks;
    return this;
  }

  public Map<String, ActionOptions> getActions() {
    return actions;
  }

  /**
   * Sets named actions with their factory configuration.
   *
   * @param actions list of named {@code Actions} (name -&gt; Action)
   * @return reference to this, so the API can be used fluently
   */
  public FragmentsHandlerOptions setActions(Map<String, ActionOptions> actions) {
    this.actions = actions;
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
    FragmentsHandlerOptions that = (FragmentsHandlerOptions) o;
    return Objects.equals(tasks, that.tasks) &&
        Objects.equals(actions, that.actions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tasks, actions);
  }

  @Override
  public String toString() {
    return "FragmentsHandlerOptions{" +
        "tasks=" + tasks +
        ", actions=" + actions +
        '}';
  }
}
