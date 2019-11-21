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

import io.knotx.fragments.task.factory.config.LogLevelConfig;
import io.knotx.fragments.task.factory.node.NodeFactoryOptions;
import io.knotx.fragments.task.options.TaskOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class DefaultTaskFactoryConfig {

  public static final String DEFAULT_TASK_NAME_KEY = "data-knotx-task";

  private List<NodeFactoryOptions> nodeFactories;
  private Map<String, TaskOptions> tasks;
  private String taskNameKey;
  private String logLevel;

  public DefaultTaskFactoryConfig() {
    init();
  }

  public DefaultTaskFactoryConfig(JsonObject json) {
    this();
    DefaultTaskFactoryConfigConverter.fromJson(json, this);
//    initNodeLogLevel(json);
  }

  private void init() {
    taskNameKey = DEFAULT_TASK_NAME_KEY;
  }

//  private void initNodeLogLevel(JsonObject json) {
//    nodeFactories.stream().flatMap(nodeFactoryOptions -> {
//      actions.values().forEach(actionOptions -> {
//        JsonObject actionConfig = actionOptions.getConfig();
//        override(actionConfig, defaultLogLevel.getLogLevel());
//      });
//      return actions;
//    }).orElseThrow(() -> new NodeConfigException(json));
//  }

  private void override(JsonObject json, String defaultLogLevel) {
    if (!StringUtils.isBlank(defaultLogLevel)) {
      LogLevelConfig logLevelConfig = new LogLevelConfig(json);
      if (StringUtils.isBlank(logLevelConfig.getLogLevel())) {
        json.mergeIn(logLevelConfig.setLogLevel(defaultLogLevel).toJson());
      }
    }
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    DefaultTaskFactoryConfigConverter.toJson(this, result);
    return result;
  }


  public Map<String, TaskOptions> getTasks() {
    return tasks;
  }

  /**
   * Sets {@code Task} list, which are named, directed graphs of {@code Actions}.
   *
   * @param tasks list of defined {@code Tasks}.
   * @return reference to this, so the API can be used fluently
   */
  public DefaultTaskFactoryConfig setTasks(Map<String, TaskOptions> tasks) {
    this.tasks = tasks;
    return this;
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

  public String getLogLevel() {
    return logLevel;
  }

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
        Objects.equals(taskNameKey, that.taskNameKey) &&
        Objects.equals(logLevel, that.logLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tasks, nodeFactories, taskNameKey, logLevel);
  }

  @Override
  public String toString() {
    return "DefaultTaskFactoryConfig{" +
        "tasks=" + tasks +
        ", nodeFactories=" + nodeFactories +
        ", taskNameKey='" + taskNameKey + '\'' +
        ", logLevel='" + logLevel + '\'' +
        '}';
  }
}
