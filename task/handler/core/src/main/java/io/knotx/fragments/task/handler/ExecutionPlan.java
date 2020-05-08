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
package io.knotx.fragments.task.handler;

import static io.knotx.fragments.task.api.Task.UNDEFINED_TASK;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.engine.FragmentEvent;
import io.knotx.fragments.task.engine.FragmentEventContext;
import io.knotx.fragments.task.api.Task;
import io.knotx.fragments.task.factory.api.metadata.TaskMetadata;
import io.knotx.fragments.task.factory.api.metadata.TaskWithMetadata;
import io.knotx.fragments.task.factory.api.metadata.TasksMetadata;
import io.knotx.server.api.context.ClientRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExecutionPlan {

  private final TaskProvider taskProvider;
  private final Map<FragmentEventContext, TaskWithMetadata> plan;

  ExecutionPlan(List<Fragment> fragments, ClientRequest clientRequest, TaskProvider taskProvider) {
    this.taskProvider = taskProvider;
    this.plan = fragments.stream()
        .map(fragment -> new FragmentEventContext(new FragmentEvent(fragment), clientRequest))
        .collect(Collectors.toMap(context -> context,
            this::getTaskWithMetadataFor,
            (u, v) -> {
              throw new IllegalStateException(String.format("Duplicate key %s", u));
            },
            LinkedHashMap::new
        ));
  }

  public Stream<Entry> getEntryStream() {
    return plan.entrySet().stream()
        .map(entry -> new Entry(entry.getKey(), entry.getValue()));
  }

  public TasksMetadata getTasksMetadata() {
    return new TasksMetadata(getEntryStream()
        .collect(Collectors.toMap(
            entry -> entry.getContext().getFragmentEvent().getFragment().getId(),
            entry -> entry.getTaskWithMetadata().getMetadata())));
  }

  private TaskWithMetadata getTaskWithMetadataFor(FragmentEventContext fragmentEventContext) {
    return taskProvider.newInstance(fragmentEventContext)
        .orElseGet(() -> new TaskWithMetadata(new Task(UNDEFINED_TASK), TaskMetadata.notDefined()));
  }

  public static class Entry {

    private final FragmentEventContext context;
    private final TaskWithMetadata taskWithMetadata;

    private Entry(FragmentEventContext context, TaskWithMetadata taskWithMetadata) {
      this.context = context;
      this.taskWithMetadata = taskWithMetadata;
    }

    public FragmentEventContext getContext() {
      return context;
    }

    public TaskWithMetadata getTaskWithMetadata() {
      return taskWithMetadata;
    }

    @Override
    public String toString() {
      return "Entry{" +
          "context=" + context +
          ", taskWithMetadata=" + taskWithMetadata +
          '}';
    }
  }
}
