package io.knotx.fragments.task;

import io.knotx.fragments.engine.Task;

public class TaskWithMetadata implements Task {

  private final String identifier;

  private final NodeWithMetadata rootNode;

  public TaskWithMetadata(String identifier, NodeWithMetadata rootNode) {
    this.identifier = identifier;
    this.rootNode = rootNode;
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public NodeWithMetadata getRootNode() {
    return rootNode;
  }

  @Override
  public String toString() {
    return "TaskWithMetadata{" +
        "identifier='" + identifier + '\'' +
        ", rootNode=" + rootNode +
        '}';
  }
}
