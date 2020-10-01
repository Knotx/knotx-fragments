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

import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.engine.TaskResult.Status;
import io.knotx.fragments.task.engine.node.NodeExecutionContext;
import io.knotx.fragments.task.engine.node.NodeResult;
import io.knotx.server.api.context.ClientRequest;

public class TaskExecutionContext {

  private final String taskName;
  private final ClientRequest clientRequest;
  private final TaskResult taskResult;
  private Node currentNode;

  public TaskExecutionContext(String taskName, FragmentContext fragmentContext, Node rootNode) {
    this.taskName = taskName;
    this.clientRequest = fragmentContext.getClientRequest();
    this.taskResult = new TaskResult(taskName, fragmentContext.getFragment());
    this.currentNode = rootNode;
  }

  public NodeExecutionContext createNodeContext() {
    // TODO: copies here
    return new NodeExecutionContext(taskName, currentNode, taskResult.getLog(),
        new FragmentContext(taskResult.getFragment(), clientRequest));
  }

  TaskResult getResult() {
    return taskResult;
  }

  Node getCurrentNode() {
    return currentNode;
  }

  boolean finished() {
    return currentNode == null;
  }

  TaskExecutionContext consumeResultAndShiftToNext(NodeResult result) {
    taskResult.consume(result);
    shiftToNext(result.getTransition());
    return this;
  }

  void handleFatal(Throwable error) {
    throw new TaskFatalException(taskResult);
  }

  private void shiftToNext(String transition) {
    currentNode = currentNode
        .next(transition)
        .orElseGet(() -> endBy(transition));
  }

  private Node endBy(String transition) {
    if (!SUCCESS_TRANSITION.equals(transition)) {
      taskResult.setStatus(Status.FAILURE);
      taskResult.getLog().unsupported(currentNode.getId(), transition);
    }
    return null;
  }

}
