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
package io.knotx.fragments.engine.graph;

import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Node {

  private String action;

  private Function<FragmentContext, Single<FragmentResult>> operation;

  private Map<String, List<Node>> outgoingEdges;

  public Node(String action, Function<FragmentContext, Single<FragmentResult>> operation,
      Map<String, List<Node>> edges) {
    this.action = action;
    this.operation = operation;
    this.outgoingEdges = edges;
  }

  public Single<FragmentResult> doOperation(FragmentContext fragmentContext) {
    return operation.apply(fragmentContext);
  }

  public List<Node> next(String transition) {
    return outgoingEdges.getOrDefault(transition, Collections.emptyList());
  }

}
