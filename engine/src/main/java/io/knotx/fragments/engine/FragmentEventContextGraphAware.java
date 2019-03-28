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

import java.util.Objects;
import java.util.Optional;

public class FragmentEventContextGraphAware {

  private final FragmentEventContext fragmentEventContext;
  private final GraphNode graphNode;

  public FragmentEventContextGraphAware(FragmentEventContext fragmentEventContext) {
    this(fragmentEventContext, null);
  }

  public FragmentEventContextGraphAware(FragmentEventContext fragmentEventContext,
      GraphNode graphNode) {
    this.fragmentEventContext = fragmentEventContext;
    this.graphNode = graphNode;
  }

  public FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }

  public Optional<GraphNode> getGraphNode() {
    return Optional.ofNullable(graphNode);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FragmentEventContextGraphAware that = (FragmentEventContextGraphAware) o;
    return Objects.equals(fragmentEventContext, that.fragmentEventContext) &&
        Objects.equals(graphNode, that.graphNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragmentEventContext, graphNode);
  }

  @Override
  public String toString() {
    return "FragmentEventContextGraphAware{" +
        "fragmentEventContext=" + fragmentEventContext +
        ", graphNode=" + graphNode +
        '}';
  }
}
