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
package io.knotx.fragments.task.factory.generic.node.action;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.single.SingleNode;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.Map;
import java.util.Optional;

class ActionNode implements SingleNode {

  private final String id;
  private final Map<String, Node> edges;
  private final Action action;

  ActionNode(String id, Map<String, Node> edges, Action action) {
    this.id = id;
    this.edges = edges;
    this.action = action;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Optional<Node> next(String transition) {
    return Optional.ofNullable(transition).map(edges::get);
  }

  @Override
  public void apply(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    action.apply(fragmentContext, resultHandler);
  }
}