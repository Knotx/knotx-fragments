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
package io.knotx.fragments.engine;

import io.knotx.fragments.engine.graph.Node;
import java.util.List;

class TaskExecutionContext {

  private final String taskName;
  private final FragmentEventContext fragmentEventContext;
  private List<Node> currentNodes;

  public TaskExecutionContext(String taskName, List<Node> graphRoot,
      FragmentEventContext fragmentEventContext) {
    this.taskName = taskName;
    this.currentNodes = graphRoot;
    this.fragmentEventContext = fragmentEventContext;
  }

  //ToDo refactor to factory method
  public TaskExecutionContext(TaskExecutionContext context, List<Node> currentNodes) {
    FragmentEvent fragmentEvent = new FragmentEvent(
        context.getFragmentEventContext().getFragmentEvent().getFragment());
    this.fragmentEventContext = new FragmentEventContext(fragmentEvent,
        context.getFragmentEventContext().getClientRequest());
    this.currentNodes = currentNodes;
    this.taskName = context.getTaskName();
  }

  FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }

  public List<Node> getCurrentNodes() {
    return currentNodes;
  }

  public TaskExecutionContext setCurrentNodes(List<Node> currentNodes) {
    this.currentNodes = currentNodes;
    return this;
  }

  public String getTaskName() {
    return taskName;
  }

}
