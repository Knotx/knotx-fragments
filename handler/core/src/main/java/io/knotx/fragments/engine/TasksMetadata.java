package io.knotx.fragments.engine;

import java.util.Map;

public class TasksMetadata {

  private Map<String, TaskMetadata> tasksMetadataByFragmentId;

  public TasksMetadata(Map<String, TaskMetadata> tasksMetadataByFragmentId) {
    this.tasksMetadataByFragmentId = tasksMetadataByFragmentId;
  }

  public TaskMetadata get(String fragmentId) {
    return tasksMetadataByFragmentId.get(fragmentId);
  }
}
