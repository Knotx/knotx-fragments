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
package io.knotx.fragments.task.exception;

import io.knotx.fragments.ConfigurationException;

public class TaskNotFoundException extends ConfigurationException {

  private String taskName;

  public TaskNotFoundException(String taskName) {
    super("Task [" + taskName + "] not configured!");
    this.taskName = taskName;
  }

  public String getTaskName() {
    return taskName;
  }
}
