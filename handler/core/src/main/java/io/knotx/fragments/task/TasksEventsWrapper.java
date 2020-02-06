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

import com.google.common.collect.Streams;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TasksEventsWrapper {

  private List<Task<NodeWithMetadata>> tasks;

  private List<FragmentEvent> fragmentEvents;

  public TasksEventsWrapper(List<Task<NodeWithMetadata>> tasks, List<FragmentEvent> fragmentEvents) {
    this.tasks = tasks;
    this.fragmentEvents = fragmentEvents;
  }

  public Stream<TaskEventWrapper> stream() {
    return Streams.zip(tasks.stream(), fragmentEvents.stream(), TaskEventWrapper::new);
  }

  public List<Task<NodeWithMetadata>> getTasks() {
    return tasks;
  }

  public List<FragmentEvent> getFragmentEvents() {
    return fragmentEvents;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TasksEventsWrapper tasksEventsWrapper = (TasksEventsWrapper) o;
    return Objects.equals(tasks, tasksEventsWrapper.tasks) &&
        Objects.equals(fragmentEvents, tasksEventsWrapper.fragmentEvents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tasks, fragmentEvents);
  }
}
