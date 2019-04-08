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

import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class GraphNode {

  private String task;

  private String action;

  private Function<FragmentContext, Single<FragmentResult>> operation;

  private Map<String, List<GraphNode>> outgoingEdges;

  public GraphNode(String task, String action,
      Function<FragmentContext, Single<FragmentResult>> operation,
      Map<String, List<GraphNode>> edges) {
    this.task = task;
    this.action = action;
    this.operation = operation;
    this.outgoingEdges = edges;
  }

  public Single<FragmentResult> doOperation(FragmentContext fragmentContext) {
    return operation.apply(fragmentContext);
  }

  public Optional<List<GraphNode>> next(String transition) {
    return Optional.ofNullable(outgoingEdges.get(transition));
  }

  public String getAction() {
    return action;
  }

  public String getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "GraphNode{" +
        "task='" + task + '\'' +
        ", action='" + action + '\'' +
        ", operation=" + operation +
        ", outgoingEdges=" + outgoingEdges +
        '}';
  }
}
