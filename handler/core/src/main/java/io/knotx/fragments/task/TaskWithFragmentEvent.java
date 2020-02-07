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

import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
import java.util.Objects;

public class TaskWithFragmentEvent {

  private Task<NodeWithMetadata> task;

  private FragmentEvent fragmentEvent;

  public TaskWithFragmentEvent(Task<NodeWithMetadata> task, FragmentEvent fragmentEvent) {
    this.task = task;
    this.fragmentEvent = fragmentEvent;
  }

  public Task<NodeWithMetadata> getTask() {
    return task;
  }

  public FragmentEvent getFragmentEvent() {
    return fragmentEvent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskWithFragmentEvent that = (TaskWithFragmentEvent) o;
    return Objects.equals(task, that.task) &&
        Objects.equals(fragmentEvent, that.fragmentEvent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(task, fragmentEvent);
  }
}
