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

import static io.knotx.fragments.handler.api.fragment.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;

import java.util.List;
import java.util.Optional;

public class CompositeNode implements Node {

  public static final String COMPOSITE_NODE_ID = "composite";
  private final List<Node> nodes;
  private final Node onSuccess;
  private final Node onError;

  public CompositeNode(List<Node> nodes, Node onSuccess, Node onError) {
    this.nodes = nodes;
    this.onSuccess = onSuccess;
    this.onError = onError;
  }

  @Override
  public String getId() {
    return COMPOSITE_NODE_ID;
  }

  @Override
  public Optional<Node> next(String transition) {
    Node nextNode = null;
    if (ERROR_TRANSITION.equals(transition)) {
      nextNode = onError;
    } else if (SUCCESS_TRANSITION.equals(transition)) {
      nextNode = onSuccess;
    }
    return Optional.ofNullable(nextNode);
  }

  public List<Node> getNodes() {
    return nodes;
  }

  @Override
  public String toString() {
    return "CompositeNode{" +
        "nodes=" + nodes +
        ", onSuccess=" + onSuccess +
        ", onError=" + onError +
        '}';
  }
}
