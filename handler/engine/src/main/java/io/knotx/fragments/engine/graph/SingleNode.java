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

import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.reactivex.Single;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class SingleNode implements Node {

  private String id;

  private Function<FragmentContext, Single<FragmentResult>> action;

  private Map<String, Node> transitions;

  public SingleNode(String id, Function<FragmentContext, Single<FragmentResult>> action) {
    this(id, action, null);
  }

  public SingleNode(String id, Function<FragmentContext, Single<FragmentResult>> action,
      Map<String, Node> transitions) {
    this.id = id;
    this.action = action;
    this.transitions = transitions;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Optional<Node> next(String transition) {
      return Optional.ofNullable(transitions).map(transitions -> transitions.get(transition));
  }

  @Override
  public NodeType getType() {
    return NodeType.SINGLE;
  }

  public Single<FragmentResult> doAction(FragmentContext fragmentContext) {
    return action.apply(fragmentContext);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingleNode that = (SingleNode) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(action, that.action) &&
        Objects.equals(transitions, that.transitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, action, transitions);
  }

  @Override
  public String toString() {
    return "SingleNode{" +
        "id='" + id + '\'' +
        ", action=" + action +
        ", transitions=" + transitions +
        '}';
  }
}
