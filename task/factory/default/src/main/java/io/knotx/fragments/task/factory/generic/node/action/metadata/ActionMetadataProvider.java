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
package io.knotx.fragments.task.factory.generic.node.action.metadata;

import static io.knotx.fragments.task.factory.api.metadata.NodeMetadata.single;

import com.google.common.collect.Maps;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;

import java.util.Map;

public class ActionMetadataProvider {

  private final OperationMetadataProvider operationMetadataProvider;

  public static ActionMetadataProvider create(String nodeFactoryName,
                                              Map<String, ActionFactoryOptions> aliasToOptions) {
    return new ActionMetadataProvider(OperationMetadataProvider.create(nodeFactoryName, aliasToOptions));
  }

  ActionMetadataProvider(OperationMetadataProvider operationMetadataProvider) {
    this.operationMetadataProvider = operationMetadataProvider;
  }

  public NodeMetadata provideFor(String nodeId, Map<String, Node> edges, String alias) {
    return single(nodeId, alias, createTransitions(edges), operationMetadataProvider.provideFor(alias));
  }

  private Map<String, String> createTransitions(Map<String, Node> edges) {
    return Maps.transformValues(edges, Node::getId);
  }

}
