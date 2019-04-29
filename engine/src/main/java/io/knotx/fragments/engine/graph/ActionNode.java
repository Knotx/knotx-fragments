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
import java.util.Optional;
import java.util.function.Function;

public class ActionNode implements Node {

  private String id;

  private Function<FragmentContext, Single<FragmentResult>> action;

  private Map<String, Node> transitions;

  public ActionNode(String id, Function<FragmentContext, Single<FragmentResult>> action,
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
    return Optional.ofNullable(transitions.get(transition));
  }

  @Override
  public boolean isComposite() {
    return false;
  }

  public Single<FragmentResult> doAction(FragmentContext fragmentContext) {
    return action.apply(fragmentContext);
  }

  @Override
  public String toString() {
    return "ActionNode{" +
        "id='" + id + '\'' +
        ", action=" + action +
        ", transitions=" + transitions +
        '}';
  }
}
