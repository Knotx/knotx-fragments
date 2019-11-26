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
package io.knotx.fragments.task.factory;

import io.knotx.fragments.task.factory.node.NodeFactoryOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class DefaultTaskFactoryConfig {

  public static final String DEFAULT_TASK_NAME_KEY = "data-knotx-task";

  private Map<String, GraphNodeOptions> tasks;
  private List<NodeFactoryOptions> nodeFactories;
  private String taskNameKey;

  public DefaultTaskFactoryConfig() {
    tasks = new HashMap<>();
    nodeFactories = new ArrayList<>();
    taskNameKey = DEFAULT_TASK_NAME_KEY;
  }

  public DefaultTaskFactoryConfig(JsonObject json) {
    this();
    DefaultTaskFactoryConfigConverter.fromJson(json, this);
    initNodeLogLevel(json);
  }

  private void initNodeLogLevel(JsonObject json) {
    LogLevelConfig globalLogLevel = new LogLevelConfig(json);
    if (StringUtils.isNotBlank(globalLogLevel.getLogLevel())) {
      nodeFactories.forEach(nodeFactoryOptions ->
          override(nodeFactoryOptions.getConfig(), globalLogLevel.getLogLevel()));
    }
  }

  private void override(JsonObject json, String globalLogLevel) {

    if (!StringUtils.isBlank(globalLogLevel)) {
      LogLevelConfig logLevelConfig = new LogLevelConfig(json);
      if (StringUtils.isBlank(logLevelConfig.getLogLevel())) {
        json.mergeIn(logLevelConfig.setLogLevel(globalLogLevel).toJson());
      }
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    DefaultTaskFactoryConfigConverter.toJson(this, result);
    return result;
  }


  public Map<String, GraphNodeOptions> getTasks() {
    return tasks;
  }

  /**
   * Sets {@code Task} list, which are named, directed graphs of {@code Actions}.
   *
   * @param tasks list of defined {@code Tasks}.
   * @return reference to this, so the API can be used fluently
   */
  public void setTasks(
      Map<String, GraphNodeOptions> tasks) {
    this.tasks = tasks;
  }

  public List<NodeFactoryOptions> getNodeFactories() {
    return nodeFactories;
  }

  public DefaultTaskFactoryConfig setNodeFactories(
      List<NodeFactoryOptions> nodeFactories) {
    this.nodeFactories = nodeFactories;
    return this;
  }

  public String getTaskNameKey() {
    return taskNameKey;
  }

  public DefaultTaskFactoryConfig setTaskNameKey(String taskNameKey) {
    this.taskNameKey = taskNameKey;
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
    DefaultTaskFactoryConfig that = (DefaultTaskFactoryConfig) o;
    return Objects.equals(tasks, that.tasks) &&
        Objects.equals(nodeFactories, that.nodeFactories) &&
        Objects.equals(taskNameKey, that.taskNameKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tasks, nodeFactories, taskNameKey);
  }

  @Override
  public String toString() {
    return "DefaultTaskFactoryConfig{" +
        "tasks=" + tasks +
        ", nodeFactories=" + nodeFactories +
        ", taskNameKey='" + taskNameKey + '\'' +
        '}';
  }
}
