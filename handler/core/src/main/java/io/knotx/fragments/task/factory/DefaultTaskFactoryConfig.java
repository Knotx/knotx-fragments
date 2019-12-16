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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import io.knotx.fragments.task.factory.node.NodeFactoryOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Default Task Factory config model.
 */
@DataObject(generateConverter = true)
public class DefaultTaskFactoryConfig {

  public static final String DEFAULT_TASK_NAME_KEY = "data-knotx-task";

  private Map<String, GraphNodeOptions> tasks;
  private List<NodeFactoryOptions> nodeFactories;
  private String taskNameKey;
  private String logLevel;

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
    LogLevelConfig globalLogLevel =
        StringUtils.isBlank(logLevel) ? new LogLevelConfig() : new LogLevelConfig(json);
    nodeFactories.forEach(nodeFactoryOptions ->
        override(nodeFactoryOptions.getConfig(), globalLogLevel.getLogLevel()));

  }

  private void override(JsonObject json, String globalLogLevel) {
    LogLevelConfig logLevelConfig = new LogLevelConfig(json);
    if (StringUtils.isBlank(logLevelConfig.getLogLevel())) {
      json.mergeIn(logLevelConfig.setLogLevel(globalLogLevel).toJson());
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
   * The dictionary that maps a task name to a directed acyclic graph (DAG) of nodes.
   *
   * @param tasks - map that links task name with its graph logic
   * @return reference to this, so the API can be used fluently
   */
  public DefaultTaskFactoryConfig setTasks(Map<String, GraphNodeOptions> tasks) {
    this.tasks = tasks;
    return this;
  }

  public List<NodeFactoryOptions> getNodeFactories() {
    return nodeFactories;
  }

  /**
   * The array/list of graph node factory options defines node factories taking part in the creation
   * of graph.
   *
   * @param nodeFactories - list of graph node factory options
   * @return reference to this, so the API can be used fluently
   */
  public DefaultTaskFactoryConfig setNodeFactories(List<NodeFactoryOptions> nodeFactories) {
    this.nodeFactories = nodeFactories;
    return this;
  }

  public String getTaskNameKey() {
    return taskNameKey;
  }

  /**
   * The fragment's configuration key specifies a task assigned to a fragment by the task name.
   *
   * @param taskNameKey - fragment's configuration key
   * @return reference to this, so the API can be used fluently
   */
  public DefaultTaskFactoryConfig setTaskNameKey(String taskNameKey) {
    this.taskNameKey = taskNameKey;
    return this;
  }


  public String getLogLevel() {
    return logLevel;
  }

  /**
   * The global node log level.
   *
   * @param logLevel - node log level
   * @return reference to this, so the API can be used fluently
   */
  public DefaultTaskFactoryConfig setLogLevel(String logLevel) {
    this.logLevel = logLevel;
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
