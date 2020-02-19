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

import static io.knotx.fragments.engine.api.node.single.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.knotx.fragments.engine.api.node.composite.CompositeNode;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.single.SingleNode;
import io.knotx.fragments.engine.api.node.single.FragmentContext;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

interface Nodes {

  static CompositeNode composite(String nodeId, List<Node> rootNodes) {
    CompositeNode node = mock(CompositeNode.class);
    when(node.getId()).thenReturn(nodeId);
    when(node.getType()).thenReturn(NodeType.COMPOSITE);
    when(node.getNodes()).thenReturn(rootNodes);
    return node;
  }

  static CompositeNode composite(String nodeId, List<Node> rootNodes, Node success) {
    CompositeNode node = composite(nodeId, rootNodes);
    when(node.next(matches(SUCCESS_TRANSITION))).thenReturn(Optional.ofNullable(success));
    return node;
  }

  static CompositeNode composite(String nodeId, List<Node> rootNodes, Node success, Node error) {
    CompositeNode node = composite(nodeId, rootNodes, success);
    when(node.next(matches(ERROR_TRANSITION))).thenReturn(Optional.ofNullable(error));
    return node;
  }

  static SingleNode single(String nodeId, Function<FragmentContext, Single<FragmentResult>> function) {
    SingleNode node = mock(SingleNode.class);
    when(node.getId()).thenReturn(nodeId);
    when(node.execute(any())).thenAnswer(
        invocation -> function.apply((FragmentContext) invocation.getArguments()[0]));
    when(node.next(any())).thenReturn(Optional.empty());
    return node;
  }

  static SingleNode single(String nodeId, Function<FragmentContext, Single<FragmentResult>> function, Map<String, Node> transitions) {
    SingleNode node = single(nodeId, function);
    transitions.forEach((key, value) -> when(node.next(matches(key)))
        .thenReturn(Optional.of(value)));
    return node;
  }

}
