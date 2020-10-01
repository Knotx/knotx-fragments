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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.api.Task;
import io.knotx.fragments.task.engine.FragmentContextTaskAware;
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
  private final Map<Fragment, TaskWithMetadata> plan;
  private final ClientRequest clientRequest;

  ExecutionPlan(List<Fragment> fragments, ClientRequest clientRequest, TaskProvider taskProvider) {
    this.taskProvider = taskProvider;
    this.clientRequest = clientRequest;
    this.plan = new LinkedHashMap<>();

    fragments.forEach(fragment -> plan.put(fragment, getTaskWithMetadataFor(fragment)));
  }

  public List<FragmentContextTaskAware> getContexts() {
    return plan.entrySet()
        .stream()
        .map(this::toContext)
        .collect(Collectors.toList());
  }

  public TasksMetadata getTasksMetadata() {
    return new TasksMetadata(fragmentIdToTaskMetadata());
  }

  private FragmentContextTaskAware toContext(Map.Entry<Fragment, TaskWithMetadata> entry) {
    return new FragmentContextTaskAware(entry.getValue().getTask(), clientRequest, entry.getKey());
  }

  private Map<String, TaskMetadata> fragmentIdToTaskMetadata() {
    return plan.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getId(),
            entry -> entry.getValue().getMetadata()));
  }

  private TaskWithMetadata getTaskWithMetadataFor(Fragment fragment) {
    return taskProvider.newInstance(fragment, clientRequest)
        .orElseGet(() -> new TaskWithMetadata(Task.undefined(), TaskMetadata.notDefined()));
  }

}
