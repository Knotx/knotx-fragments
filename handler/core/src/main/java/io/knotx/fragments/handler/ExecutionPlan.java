package io.knotx.fragments.handler;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.*;
import io.knotx.fragments.engine.api.Task;
import io.knotx.server.api.context.ClientRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExecutionPlan {

  TaskProvider taskProvider;
  Map<FragmentEventContext, TaskWithMetadata> plan;

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
        .orElseGet(() -> new TaskWithMetadata(new Task("_NOT_DEFINED"), TaskMetadata.notDefined()));
  }

  public static class Entry {
    private FragmentEventContext context;
    private TaskWithMetadata taskWithMetadata;

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
  }
}
