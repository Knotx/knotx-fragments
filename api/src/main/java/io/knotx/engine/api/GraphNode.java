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
package io.knotx.engine.api;

import io.knotx.engine.api.fragment.FragmentContext;
import io.knotx.engine.api.fragment.FragmentResult;
import io.reactivex.Single;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GraphNode {

  private String identifier;

  private Function<FragmentContext, Single<FragmentResult>> operation;

  private Map<String, GraphNode> outgoingEdges;

  public GraphNode(String identifier, Function<FragmentContext, Single<FragmentResult>> operation,
      Map<String, GraphNode> edges) {
    this.identifier = identifier;
    this.operation = operation;
    this.outgoingEdges = edges;
  }

  public Single<FragmentResult> doOperation(FragmentContext fragmentContext) {
    return operation.apply(fragmentContext);
  }

  public Optional<GraphNode> next(String transition) {
    return Optional.ofNullable(outgoingEdges.get(transition));
  }

  public String getName() {
    return identifier;
  }
}
