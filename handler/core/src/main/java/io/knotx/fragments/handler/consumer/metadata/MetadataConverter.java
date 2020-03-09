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
package io.knotx.fragments.handler.consumer.metadata;

import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.TaskMetadata;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.handler.LoggedNodeStatus;
import io.knotx.fragments.handler.consumer.html.GraphNodeExecutionLog;
import io.knotx.fragments.handler.consumer.html.GraphNodeResponseLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MetadataConverter {

  private final String rootNodeId;
  private final Map<String, NodeMetadata> nodes;
  private final EventLogConverter eventLogConverter;

  public MetadataConverter(FragmentEvent event, TaskMetadata taskMetadata) {
    this.rootNodeId = taskMetadata.getRootNodeId();
    this.nodes = taskMetadata.getNodesMetadata();
    this.eventLogConverter = new EventLogConverter(event.getLog().getOperations());
  }

  public GraphNodeExecutionLog createNode() {
    return createNodeJson(rootNodeId);
  }

  private GraphNodeExecutionLog createNodeJson(String id) {
    GraphNodeExecutionLog graphLog = fillWithMetadata(id);
    NodeExecutionData nodeExecutionData = eventLogConverter.fillWithLog(id);
    graphLog.setStatus(nodeExecutionData.getStatus());
    Optional.ofNullable(nodeExecutionData.getResponse())
        .map(response -> graphLog
            .setResponse(GraphNodeResponseLog.newInstance(response.getTransition(),
                response.getInvocations())));
    if (isMissing(graphLog)) {
      graphLog.getOn().put(graphLog.getResponse().getTransition(), missingNodeData());
    }
    return graphLog;
  }

  private boolean isMissing(GraphNodeExecutionLog graphLog) {
    String transition = graphLog.getResponse().getTransition();
    return transition != null
        && !SUCCESS_TRANSITION.equals(transition)
        && !graphLog.getOn().containsKey(transition);
  }

  private GraphNodeExecutionLog missingNodeData() {
    return GraphNodeExecutionLog
        .newInstance(UUID.randomUUID().toString(), NodeType.SINGLE, "!",
            Collections.emptyList(), null, Collections.emptyMap())
        .setStatus(LoggedNodeStatus.MISSING);
  }

  private GraphNodeExecutionLog fillWithMetadata(String id) {
    if (nodes.containsKey(id)) {
      NodeMetadata metadata = nodes.get(id);
      return GraphNodeExecutionLog
          .newInstance(metadata.getNodeId(),
              metadata.getType(),
              metadata.getLabel(),
              getSubTasks(metadata.getNestedNodes()),
              metadata.getOperation(),
              getTransitions(metadata.getTransitions()));
    } else {
      return GraphNodeExecutionLog.newInstance(id);
    }
  }

  private Map<String, GraphNodeExecutionLog> getTransitions(
      Map<String, String> definedTransitions) {
    Map<String, GraphNodeExecutionLog> result = new HashMap<>();
    definedTransitions.forEach((name, nextId) -> result.put(name, createNodeJson(nextId)));
    return result;
  }

  private List<GraphNodeExecutionLog> getSubTasks(List<String> nestedNodes) {
    return nestedNodes.stream()
        .map(this::createNodeJson)
        .collect(Collectors.toList());
  }

}
