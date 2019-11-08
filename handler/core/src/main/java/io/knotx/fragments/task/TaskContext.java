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
package io.knotx.fragments.task;

import io.knotx.fragments.engine.FragmentEventContext;

public class TaskContext {

  private TaskDefinition taskDefinition;
  private FragmentEventContext fragmentEventContext;

  public TaskContext(TaskDefinition taskDefinition, FragmentEventContext fragmentEventContext) {
    this.taskDefinition = taskDefinition;
    this.fragmentEventContext = fragmentEventContext;
  }

  public TaskDefinition getTaskDefinition() {
    return taskDefinition;
  }

  public FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }

  @Override
  public String toString() {
    return "TaskContext{" +
        "taskDefinition=" + taskDefinition +
        ", fragmentEventContext=" + fragmentEventContext +
        '}';
  }
}
