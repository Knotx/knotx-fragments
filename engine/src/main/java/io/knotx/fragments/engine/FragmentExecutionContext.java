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

class FragmentExecutionContext {

  private FragmentEventContext fragmentEventContext;

  private Node current;

  public FragmentExecutionContext() {
    //empty
  }

  public FragmentExecutionContext(FragmentExecutionContext context, Node current) {
    FragmentEvent fragmentEvent = new FragmentEvent(context.getFragmentEventContext().getFragmentEvent().getFragment());
    this.fragmentEventContext = new FragmentEventContext(fragmentEvent, context.getFragmentEventContext().getClientRequest());
    this.current = current;
  }

  FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }

  FragmentExecutionContext setFragmentEventContext(
      FragmentEventContext fragmentEventContext) {
    this.fragmentEventContext = fragmentEventContext;
    return this;
  }

  Node getCurrent() {
    return current;
  }

  FragmentExecutionContext setCurrent(Node current) {
    this.current = current;
    return this;
  }

  FragmentExecutionContext end() {
    current = null;
    return this;
  }

  boolean isLast() {
    return current == null;
  }

}
