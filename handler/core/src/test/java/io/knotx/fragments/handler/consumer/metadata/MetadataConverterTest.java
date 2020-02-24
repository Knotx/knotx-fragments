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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.EventLog;
import io.knotx.fragments.engine.EventLogEntry;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.NodeMetadata;
import io.knotx.fragments.engine.TaskMetadata;
import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataConverterTest {

  private static final String TASK_NAME = "some-task";
  private static final String ROOT_NODE = "1234-4321-1234";

  private MetadataConverter tested;

  @Test
  @DisplayName("Expect empty json when not defined TaskMetadata provided")
  void shouldProduceEmptyJsonWhenNoMetadataProvided() {
    givenNotDefinedTaskMetadata();

    JsonObject output = tested.createJson();

    assertEquals(new JsonObject(), output);
  }

  @Test
  @DisplayName("Expect only processing info and node id when TaskMetadata with no metadata provided")
  void shouldProduceOnlyProcessingInfoWhenNoMetadataProvided() {
    givenNoMetadata(ROOT_NODE);

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForNotDescribedNode(ROOT_NODE);

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with one node provided")
  void shouldProduceCorrectJsonForOneNodeMetadata() {
    givenNodesMetadata(ROOT_NODE, simpleNode(ROOT_NODE, "custom"));

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForNode(ROOT_NODE);

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with one action node provided")
  void shouldProduceCorrectJsonForOneActionNodeMetadata() {
    givenNodesMetadata(ROOT_NODE, simpleNode(ROOT_NODE, "action"));

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForActionNode(ROOT_NODE);

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with two nodes with transition provided")
  void shouldProduceCorrectJsonForTwoNodesWithTransition() {
    givenNodesMetadata(ROOT_NODE,
        simpleNode(ROOT_NODE, "custom", ImmutableMap.of("_success", "node-A")),
        simpleNode("node-A", "custom")
    );

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForNode(ROOT_NODE)
        .put("on", new JsonObject()
            .put("_success", jsonForNode("node-A")));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with nested nodes provided")
  void shouldProduceCorrectJsonForCompositeNodeWithSimpleNodes() {
    givenNodesMetadata(ROOT_NODE,
        compositeNode(ROOT_NODE, "custom", "node-A", "node-B", "node-C"),
        simpleNode("node-A", "factory-A"),
        simpleNode("node-B", "factory-B"),
        simpleNode("node-C", "factory-C")
    );

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForNode(ROOT_NODE)
        .put("type", NodeType.COMPOSITE)
        .put("subtasks", new JsonArray(Arrays.asList(
            jsonForNode("node-A"),
            jsonForNode("node-B"),
            jsonForNode("node-C")
        )));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with nested nodes provided and some are not described")
  void shouldProduceCorrectJsonForCompositeNodeWithSimpleNodesNotAllDescribed() {
    givenNodesMetadata(ROOT_NODE,
        compositeNode(ROOT_NODE, "custom", "node-A", "node-B", "node-C"),
        simpleNode("node-A", "factory-A")
    );

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForNode(ROOT_NODE)
        .put("type", NodeType.COMPOSITE)
        .put("subtasks", new JsonArray(Arrays.asList(
            jsonForNode("node-A"),
            jsonForNotDescribedNode("node-B"),
            jsonForNotDescribedNode("node-C")
        )));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when full metadata for complex graph provided")
  void shouldProduceCorrectJsonForComplexGraphWithFullMetadata() {
    givenNodesMetadata(ROOT_NODE,
        simpleNode(ROOT_NODE, "custom",
            ImmutableMap.of("_success", "node-A", "_failure", "node-B")),
        simpleNode("node-A", "factory-A"),
        compositeNode("node-B", "factory-B",
            ImmutableMap.of("_success", "node-C", "_error", "node-D", "_timeout", "node-E"),
            "node-B1", "node-B2", "node-B3"
        ),
        simpleNode("node-B1", "action", ImmutableMap.of("_success", "node-B1-special")),
        simpleNode("node-B1-special", "factory-BB-special"),
        simpleNode("node-B2", "factory-BB"),
        simpleNode("node-B3", "factory-BB"),
        simpleNode("node-C", "factory-C"),
        simpleNode("node-D", "factory-D"),
        simpleNode("node-E", "factory-E")
    );

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForNode(ROOT_NODE)
        .put("on", new JsonObject()
            .put("_success", jsonForNode("node-A"))
            .put("_failure", jsonForNode("node-B")
                .put("type", NodeType.COMPOSITE)
                .put("on", new JsonObject()
                    .put("_success", jsonForNode("node-C"))
                    .put("_error", jsonForNode("node-D"))
                    .put("_timeout", jsonForNode("node-E"))
                )
                .put("subtasks", new JsonArray(Arrays.asList(
                    jsonForActionNode("node-B1")
                        .put("on", new JsonObject()
                            .put("_success", jsonForNode("node-B1-special"))),
                    jsonForNode("node-B2"),
                    jsonForNode("node-B3")
                )))
            ));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when full metadata for complex graph provided with event log")
  void shouldProduceCorrectJsonForComplexGraphWithFullMetadataWithEventLog() {
    EventLog eventLog = createEventLog(
        EventLogEntry
            .success(TASK_NAME, "a-node", createFragmentResult("_success", simpleNodeLog())),
        EventLogEntry
            .success(TASK_NAME, "b1-subgraph", createFragmentResult("_success", simpleNodeLog())),
        EventLogEntry
            .success(TASK_NAME, "b2-subgraph", createFragmentResult("_fallback", wrappedNodeLog())),
        EventLogEntry.unsupported(TASK_NAME, "b2-subgraph", "_fallback"),
        EventLogEntry
            .success(TASK_NAME, "b-composite", createFragmentResult("_error", simpleNodeLog())),
        EventLogEntry
            .success(TASK_NAME, "f-node", createFragmentResult("_success", simpleNodeLog()))
    );

    givenEventLogAndNodesMetadata(
        eventLog,
        "a-node",
        simpleNode("a-node", "action",
            ImmutableMap.of("_success", "b-composite", "_error", "c-node")),
        compositeNode("b-composite", "factory-B",
            ImmutableMap.of("_success", "e-node", "_error", "f-node"),
            "b1-subgraph", "b2-subgraph"
        ),
        simpleNode("b1-subgraph", "action"),
        simpleNode("b2-subgraph", "action", ImmutableMap.of("_success", "d-node")),
        simpleNode("c-node", "action"),
        simpleNode("d-node", "action"),
        simpleNode("e-node", "action"),
        simpleNode("f-node", "action")
    );

    JsonObject output = tested.createJson();

    JsonObject expected = jsonForActionNode("a-node")
        .put("response", new JsonObject()
            .put("transition", "_success")
            .put("invocations", simpleNodeLog()))
        .put("status", "SUCCESS")
        .put("_logStatus", "ok")
        .put("on", new JsonObject()
            .put("_error", jsonForActionNode("c-node"))
            .put("_success", jsonForNode("b-composite")
                .put("response", new JsonObject()
                    .put("transition", "_error")
                    .put("invocations", simpleNodeLog()))
                .put("status", "SUCCESS")
                .put("_logStatus", "ok")
                .put("type", NodeType.COMPOSITE)
                .put("on", new JsonObject()
                    .put("_success", jsonForActionNode("e-node"))
                    .put("_error", jsonForActionNode("f-node")
                        .put("response", new JsonObject()
                            .put("transition", "_success")
                            .put("invocations", simpleNodeLog()))
                        .put("status", "SUCCESS")
                        .put("_logStatus", "ok")
                    )
                )
                .put("subtasks", new JsonArray(Arrays.asList(
                    jsonForActionNode("b1-subgraph")
                        .put("response", new JsonObject()
                            .put("transition", "_success")
                            .put("invocations", simpleNodeLog()))
                        .put("status", "SUCCESS")
                        .put("_logStatus", "ok"),
                    jsonForActionNode("b2-subgraph")
                        .put("on", new JsonObject()
                            .put("_success", jsonForActionNode("d-node")))
                        .put("response", new JsonObject()
                            .put("transition", "_fallback")
                            .put("invocations", wrappedNodeLog()))
                        .put("status", "UNSUPPORTED_TRANSITION")
                        .put("_logStatus", "ok")
                )))
            ));

    assertEquals(expected, output);
  }

  private EventLog createEventLog(EventLogEntry... entries) {
    return new EventLog(new JsonObject()
        .put("operations", new JsonArray(
            Arrays.stream(entries).map(EventLogEntry::toJson).collect(Collectors.toList()))));
  }

  private FragmentResult createFragmentResult(String transition, JsonObject nodeLog) {
    return new FragmentResult(new Fragment("_empty", new JsonObject(), ""), transition, nodeLog);
  }

  private void givenNotDefinedTaskMetadata() {
    tested = MetadataConverter.from(emptyFragmentEvent(), TaskMetadata.notDefined());
  }

  private void givenNoMetadata(String rootNodeId) {
    tested = MetadataConverter
        .from(emptyFragmentEvent(), TaskMetadata.noMetadata(TASK_NAME, rootNodeId));
  }

  private void givenNodesMetadata(String rootNodeId, NodeMetadata... nodes) {
    givenEventLogAndNodesMetadata(new EventLog(), rootNodeId, nodes);
  }

  private void givenEventLogAndNodesMetadata(EventLog eventLog, String rootNodeId,
      NodeMetadata... nodes) {
    Map<String, NodeMetadata> metadata = new HashMap<>();
    for (NodeMetadata node : nodes) {
      metadata.put(node.getNodeId(), node);
    }

    tested = MetadataConverter
        .from(emptyFragmentEvent(eventLog), TaskMetadata.create(TASK_NAME, rootNodeId, metadata));
  }

  private FragmentEvent emptyFragmentEvent() {
    return emptyFragmentEvent(new EventLog());
  }

  private FragmentEvent emptyFragmentEvent(EventLog eventLog) {
    FragmentEvent output = new FragmentEvent(new Fragment("dummy", new JsonObject(), ""));
    output.appendLog(eventLog);
    return output;
  }

  private NodeMetadata simpleNode(String id, String factory) {
    return simpleNode(id, factory, ImmutableMap.of());
  }

  private NodeMetadata simpleNode(String id, String factory, Map<String, String> transitions) {
    return new NodeMetadata(
        id,
        factory,
        NodeType.SINGLE,
        transitions,
        Collections.emptyList(),
        getSampleConfigFor(factory)
    );
  }

  private JsonObject getSampleConfigFor(String factory) {
    if ("action".equals(factory)) {
      return new JsonObject()
          .put("factory", "http")
          .put("type", "action");
    } else {
      return new JsonObject();
    }
  }

  private NodeMetadata compositeNode(String id, String factory, String... nested) {
    return compositeNode(id, factory, ImmutableMap.of(), nested);
  }

  private NodeMetadata compositeNode(String id, String factory, Map<String, String> transitions,
      String... nested) {
    return new NodeMetadata(
        id,
        factory,
        NodeType.COMPOSITE,
        transitions,
        Arrays.asList(nested),
        new JsonObject()
    );
  }

  private JsonObject jsonForNode(String id) {
    return new JsonObject()
        .put("id", id)
        .put("label", id)
        .put("type", NodeType.SINGLE)
        .put("on", new JsonObject())
        .put("subtasks", new JsonArray())
        .put("operation", new JsonObject())
        .put("_logStatus", "missing")
        .put("_metadataStatus", "ok");
  }

  private JsonObject jsonForActionNode(String id) {
    return jsonForNode(id)
        .put("operation", new JsonObject()
            .put("factory", "http")
            .put("type", "action"));
  }

  private JsonObject jsonForNotDescribedNode(String id) {
    return new JsonObject()
        .put("id", id)
        .put("_logStatus", "missing")
        .put("_metadataStatus", "missing");
  }

  private JsonObject simpleNodeLog() {
    return new JsonObject()
        .put("alias", "alias")
        .put("started", 123123213)
        .put("finished", 21342342)
        .put("operation", "http")
        .put("logs", new JsonObject());
  }

  private JsonObject wrappedNodeLog() {
    return new JsonObject()
        .put("alias", "cb-my-payments")
        .put("started", 123123213)
        .put("finished", 21342342)
        .put("operation", new JsonObject()
            .put("type", "action")
            .put("factory", "cb"))
        .put("logs", new JsonObject())
        .put("invocations", new JsonArray(Arrays.asList(
            new JsonObject()
                .put("alias", "my-payments")
                .put("started", 123123213)
                .put("finished", 21342342)
                .put("operation", new JsonObject()
                    .put("type", "action")
                    .put("factory", "http"))
                .put("logs", new JsonObject()
                    .put("request", new JsonObject())
                    .put("response", new JsonObject()
                        .put("statusCode", 500))),
            new JsonObject()
                .put("alias", "my-payments")
                .put("started", 123123213)
                .put("finished", 21342342)
                .put("operation", new JsonObject()
                    .put("type", "action")
                    .put("factory", "http"))
                .put("logs", new JsonObject()
                    .put("request", new JsonObject())
                    .put("response", new JsonObject()
                        .put("statusCode", 500)))
        )
        ));
  }

}
