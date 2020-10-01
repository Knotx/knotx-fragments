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
package io.knotx.fragments.task.engine;

import io.knotx.fragments.task.api.NodeFatalException;

/**
 * When {@link TaskEngine} processes a fragment and executes a task, then when a node responds with
 * {@link NodeFatalException} then this exception is propagated to providing more context details.
 * Both exceptions are propagated via {@link io.reactivex.exceptions.CompositeException}.
 */
public class TaskFatalException extends IllegalStateException {

  private final TaskResult event;

  TaskFatalException(TaskResult event) {
    this.event = event;
  }

  /**
   * Fragment event context. Please note that for a composite node, this context does not cover
   * other subtasks.
   *
   * @return event context
   */
  public TaskResult getEvent() {
    return event;
  }
}
