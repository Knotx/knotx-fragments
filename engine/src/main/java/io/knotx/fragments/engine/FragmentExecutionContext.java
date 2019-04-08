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

import java.util.List;
import java.util.Set;

class FragmentExecutionContext {

  private FragmentEventContext fragmentEventContext;

  private List<GraphNode> graphNodes;

  FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }

  FragmentExecutionContext setFragmentEventContext(
      FragmentEventContext fragmentEventContext) {
    this.fragmentEventContext = fragmentEventContext;
    return this;
  }

  List<GraphNode> getGraphNodes() {
    return graphNodes;
  }

  FragmentExecutionContext setGraphNodes(List<GraphNode> graphNodes) {
    this.graphNodes = graphNodes;
    return this;
  }

  FragmentExecutionContext end() {
    graphNodes = null;
    return this;
  }

  boolean isLast() {
    return graphNodes == null;
  }

}
