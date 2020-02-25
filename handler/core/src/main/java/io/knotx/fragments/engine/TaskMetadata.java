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
package io.knotx.fragments.engine;

import java.util.HashMap;
import java.util.Map;

public class TaskMetadata {

  private String taskName;
  private String rootNodeId;
  private Map<String, NodeMetadata> nodesMetadata;

  private TaskMetadata(String taskName, String rootNodeId,
      Map<String, NodeMetadata> nodesMetadata) {
    this.taskName = taskName;
    this.rootNodeId = rootNodeId;
    this.nodesMetadata = nodesMetadata;
  }

  public static TaskMetadata create(String taskName, String rootNodeId,
      Map<String, NodeMetadata> nodesMetadata) {
    return new TaskMetadata(taskName, rootNodeId, nodesMetadata);
  }

  public static TaskMetadata noMetadata(String taskName, String rootNodeId) {
    return new TaskMetadata(taskName, rootNodeId, new HashMap<>());
  }

  public static TaskMetadata notDefined() {
    return noMetadata("_NOT_DEFINED", "");
  }

  public String getTaskName() {
    return taskName;
  }

  public String getRootNodeId() {
    return rootNodeId;
  }

  public Map<String, NodeMetadata> getNodesMetadata() {
    return nodesMetadata;
  }

  @Override
  public String toString() {
    return "TaskMetadata{" +
        "taskName='" + taskName + '\'' +
        ", rootNodeId='" + rootNodeId + '\'' +
        ", nodesMetadata=" + nodesMetadata +
        '}';
  }
}
