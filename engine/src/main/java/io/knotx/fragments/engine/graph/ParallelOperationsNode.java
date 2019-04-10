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

import static io.knotx.fragments.handler.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;

import java.util.Optional;
import java.util.Set;

public class ParallelOperationsNode implements Node {

  private final Set<Node> parallelNodes;
  private final Node successTransition;
  private final Node errorTransition;

  public ParallelOperationsNode(Set<Node> parallelNodes,
      Node successTransition, Node errorTransition) {
    this.parallelNodes = parallelNodes;
    this.successTransition = successTransition;
    this.errorTransition = errorTransition;
  }

  @Override
  public Optional<Node> next(String transition) {
    Node nextNode = null;
    if (ERROR_TRANSITION.equals(transition)) {
      nextNode = errorTransition;
    } else if (DEFAULT_TRANSITION.equals(transition)) {
      nextNode = successTransition;
    }

    return Optional.ofNullable(nextNode);
  }

  public Set<Node> getParallelNodes() {
    return parallelNodes;
  }

  @Override
  public String toString() {
    return "ParallelOperationsNode{" +
        "parallelNodes=" + parallelNodes +
        ", successTransition=" + successTransition +
        ", errorTransition=" + errorTransition +
        '}';
  }
}
