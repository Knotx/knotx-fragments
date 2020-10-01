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
package io.knotx.fragments.task.handler.consumer;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;

import com.google.common.collect.ImmutableMap;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.task.engine.EventLog;
import io.knotx.fragments.task.engine.EventLogEntry;
import io.knotx.fragments.task.engine.TaskResult;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.knotx.fragments.task.factory.api.metadata.TaskMetadata;
import io.knotx.fragments.task.handler.log.api.model.GraphNodeOperationLog;
import io.knotx.fragments.task.handler.log.api.model.LoggedNodeStatus;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    JsonObject output = tested.getExecutionLog().toJson();

    assertJsonEquals(new JsonObject(), output);
  }

  @Test
  @DisplayName("Expect only processing info and node id when TaskMetadata with no metadata provided")
  void shouldProduceOnlyProcessingInfoWhenNoMetadataProvided() {
    givenNoMetadata(ROOT_NODE);

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNotDescribedNode(ROOT_NODE);

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with one node provided")
  void shouldProduceCorrectJsonForOneNodeMetadata() {
    givenNodesMetadata(ROOT_NODE, singleNode(ROOT_NODE, "custom"));

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom");

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with one action node provided")
  void shouldProduceCorrectJsonForOneActionNodeMetadata() {
    givenNodesMetadata(ROOT_NODE, singleNode(ROOT_NODE, "action"));

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForActionNode(ROOT_NODE);

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with two nodes with transition provided")
  void shouldProduceCorrectJsonForTwoNodesWithTransition() {
    givenNodesMetadata(ROOT_NODE,
        singleNode(ROOT_NODE, "custom", ImmutableMap.of(SUCCESS_TRANSITION, "node-A")),
        singleNode("node-A", "factory-A")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("on", new JsonObject()
            .put(SUCCESS_TRANSITION, jsonForNode("node-A", "factory-A")));

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect missing node metadata when node ends with _error transition.")
  void shouldProduceCorrectJsonForMissingNodeCase() {
    JsonObject nodeLog = simpleNodeLog();

    EventLog log = new EventLog(TASK_NAME);

    log.error(ROOT_NODE, ERROR_TRANSITION, nodeLog);
    log.unsupported(ROOT_NODE, ERROR_TRANSITION);

    givenEventLogAndNodesMetadata(log, ROOT_NODE,
        singleNode(ROOT_NODE, "custom", ImmutableMap.of(SUCCESS_TRANSITION, "node-A")),
        singleNode("node-A", "factory-A")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    String missingNodeId = output.getJsonObject("on").getJsonObject(ERROR_TRANSITION)
        .getString("id");

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("status", LoggedNodeStatus.ERROR)
        .put("on", new JsonObject()
            .put(SUCCESS_TRANSITION, jsonForNode("node-A", "factory-A"))
            .put(ERROR_TRANSITION, jsonForMissingNode(missingNodeId)))
        .put("response", new JsonObject()
            .put("transition", ERROR_TRANSITION)
            .put("log", nodeLog));

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect no missing node metadata when _success transition and no next node defined")
  void shouldProduceCorrectJsonForSuccessTransitionWithoutNextNode() {
    EventLog log = new EventLog(TASK_NAME);
    log.success(ROOT_NODE, createFragmentResult(SUCCESS_TRANSITION, simpleNodeLog()));

    givenEventLogAndNodesMetadata(log, ROOT_NODE,
        singleNode(ROOT_NODE, "custom")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("status", LoggedNodeStatus.SUCCESS)
        .put("response", new JsonObject()
            .put("transition", SUCCESS_TRANSITION)
            .put("log", simpleNodeLog()));

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect errors in response when node throws an exception.")
  void shouldProduceCorrectJsonForExceptionFromNode() {
    CompositeException error = compositeError();
    EventLog log = new EventLog(TASK_NAME);
    log.exception(ROOT_NODE, ERROR_TRANSITION, error);
    log.unsupported(ROOT_NODE, ERROR_TRANSITION);

    givenEventLogAndNodesMetadata(log, ROOT_NODE,
        singleNode(ROOT_NODE, "custom", ImmutableMap.of("custom", "node-A")),
        singleNode("node-A", "factory-A")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("status", LoggedNodeStatus.ERROR)
        .put("on", new JsonObject()
            .put("custom", jsonForNode("node-A", "factory-A")))
        .put("response", new JsonObject()
            .put("transition", ERROR_TRANSITION)
            .put("log", new JsonObject())
            .put("errors", new JsonArray().add(
                new JsonObject().put("className", "java.lang.IllegalArgumentException")
                    .put("message", "error message 1")).add(
                new JsonObject().put("className", "java.lang.IllegalStateException")
                    .put("message", "error message 2"))
            ));

    assertJsonEquals(expected, output);
  }


  @Test
  @DisplayName("Expect correct JSON when metadata with nested nodes provided")
  void shouldProduceCorrectJsonForCompositeNodeWithSimpleNodes() {
    givenNodesMetadata(ROOT_NODE,
        compositeNode(ROOT_NODE, "custom", "node-A", "node-B", "node-C"),
        singleNode("node-A", "factory-A"),
        singleNode("node-B", "factory-B"),
        singleNode("node-C", "factory-C")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("type", NodeType.COMPOSITE)
        .put("label", "composite")
        .put("subtasks", new JsonArray(Arrays.asList(
            jsonForNode("node-A", "factory-A"),
            jsonForNode("node-B", "factory-B"),
            jsonForNode("node-C", "factory-C")
        )));

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when metadata with nested nodes provided and some are not described")
  void shouldProduceCorrectJsonForCompositeNodeWithSimpleNodesNotAllDescribed() {
    givenNodesMetadata(ROOT_NODE,
        compositeNode(ROOT_NODE, "custom", "node-A", "node-B", "node-C"),
        singleNode("node-A", "factory-A"),
        singleNode("node-B", "factory-B"),
        singleNode("node-C", "factory-C")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("type", NodeType.COMPOSITE)
        .put("label", "composite")
        .put("subtasks", new JsonArray(Arrays.asList(
            jsonForNode("node-A", "factory-A"),
            jsonForNotDescribedNode("node-B"),
            jsonForNotDescribedNode("node-C")
        )));

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when full metadata for complex graph provided")
  void shouldProduceCorrectJsonForComplexGraphWithFullMetadata() {
    givenNodesMetadata(ROOT_NODE,
        singleNode(ROOT_NODE, "custom",
            ImmutableMap.of(SUCCESS_TRANSITION, "node-A", "_failure", "node-B")),
        singleNode("node-A", "factory-A"),
        compositeSubtasksNode("node-B",
            ImmutableMap
                .of(SUCCESS_TRANSITION, "node-C", ERROR_TRANSITION, "node-D", "_timeout", "node-E"),
            "node-B1", "node-B2", "node-B3"
        ),
        singleNode("node-B1", "action", ImmutableMap.of(SUCCESS_TRANSITION, "node-B1-special")),
        singleNode("node-B1-special", "factory-B1-special"),
        singleNode("node-B2", "factory-B2"),
        singleNode("node-B3", "factory-B3"),
        singleNode("node-C", "factory-C"),
        singleNode("node-D", "factory-D"),
        singleNode("node-E", "factory-E")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    JsonObject expected = jsonForNode(ROOT_NODE, "custom")
        .put("on", new JsonObject()
            .put(SUCCESS_TRANSITION, jsonForNode("node-A", "factory-A"))
            .put("_failure", jsonForNode("node-B", "subtasks")
                .put("type", NodeType.COMPOSITE)
                .put("label", "composite")
                .put("on", new JsonObject()
                    .put(SUCCESS_TRANSITION, jsonForNode("node-C", "factory-C"))
                    .put(ERROR_TRANSITION, jsonForNode("node-D", "factory-D"))
                    .put("_timeout", jsonForNode("node-E", "factory-E"))
                )
                .put("subtasks", new JsonArray(Arrays.asList(
                    jsonForActionNode("node-B1")
                        .put("on", new JsonObject()
                            .put(SUCCESS_TRANSITION,
                                jsonForNode("node-B1-special", "factory-B1-special"))),
                    jsonForNode("node-B2", "factory-B2"),
                    jsonForNode("node-B3", "factory-B3")
                )))
            ));

    assertJsonEquals(expected, output);
  }

  @Test
  @DisplayName("Expect correct JSON when full metadata for complex graph provided with event log")
  void shouldProduceCorrectJsonForComplexGraphWithFullMetadataWithEventLog() {
    EventLog log = new EventLog(TASK_NAME);

    log.success("a-node", createFragmentResult(SUCCESS_TRANSITION, simpleNodeLog()));
    log.success("b1-subgraph", createFragmentResult(SUCCESS_TRANSITION, simpleNodeLog()));
    log.success("b2-subgraph", createFragmentResult("_fallback", complexNodeLog()));
    log.unsupported("b2-subgraph", "_fallback");
    log.error("b-composite", ERROR_TRANSITION);
    log.success("f-node", createFragmentResult(SUCCESS_TRANSITION, simpleNodeLog()));

    givenEventLogAndNodesMetadata(
        log,
        "a-node",
        singleNode("a-node", "action",
            ImmutableMap.of(SUCCESS_TRANSITION, "b-composite", ERROR_TRANSITION, "c-node")),
        compositeSubtasksNode("b-composite",
            ImmutableMap.of(SUCCESS_TRANSITION, "e-node", ERROR_TRANSITION, "f-node"),
            "b1-subgraph", "b2-subgraph"
        ),
        singleNode("b1-subgraph", "action"),
        singleNode("b2-subgraph", "action", ImmutableMap.of(SUCCESS_TRANSITION, "d-node")),
        singleNode("c-node", "action"),
        singleNode("d-node", "action"),
        singleNode("e-node", "action"),
        singleNode("f-node", "action")
    );

    JsonObject output = tested.getExecutionLog().toJson();

    String missingNodeId = output.getJsonObject("on").getJsonObject(SUCCESS_TRANSITION)
        .getJsonArray("subtasks").getJsonObject(1).getJsonObject("on").getJsonObject("_fallback")
        .getString("id");

    JsonObject expected = jsonForActionNode("a-node")
        .put("response", new JsonObject()
            .put("transition", SUCCESS_TRANSITION)
            .put("log", simpleNodeLog()))
        .put("status", LoggedNodeStatus.SUCCESS)
        .put("on", new JsonObject()
            .put(ERROR_TRANSITION, jsonForActionNode("c-node"))
            .put(SUCCESS_TRANSITION, jsonForNode("b-composite", "subtasks")
                .put("response", new JsonObject()
                    .put("transition", ERROR_TRANSITION)
                    .put("log", new JsonObject()))
                .put("status", LoggedNodeStatus.ERROR)
                .put("type", NodeType.COMPOSITE)
                .put("label", "composite")
                .put("on", new JsonObject()
                    .put(SUCCESS_TRANSITION, jsonForActionNode("e-node"))
                    .put(ERROR_TRANSITION, jsonForActionNode("f-node")
                        .put("response", new JsonObject()
                            .put("transition", SUCCESS_TRANSITION)
                            .put("log", simpleNodeLog()))
                        .put("status", LoggedNodeStatus.SUCCESS)
                    )
                )
                .put("subtasks", new JsonArray(Arrays.asList(
                    jsonForActionNode("b1-subgraph")
                        .put("response", new JsonObject()
                            .put("transition", SUCCESS_TRANSITION)
                            .put("log", simpleNodeLog()))
                        .put("status", LoggedNodeStatus.SUCCESS),
                    jsonForActionNode("b2-subgraph")
                        .put("on", new JsonObject()
                            .put(SUCCESS_TRANSITION, jsonForActionNode("d-node"))
                            .put("_fallback", jsonForMissingNode(missingNodeId)))
                        .put("response", new JsonObject()
                            .put("transition", "_fallback")
                            .put("log", complexNodeLog()))
                        .put("status", LoggedNodeStatus.OTHER)
                )))
            ));

    assertJsonEquals(expected, output);
  }

  private FragmentResult createFragmentResult(String transition, JsonObject nodeLog) {
    return FragmentResult.success(new Fragment("_empty", new JsonObject(), ""), transition, nodeLog);
  }

  private void givenNotDefinedTaskMetadata() {
    tested = new MetadataConverter(emptyFragmentEvent(), TaskMetadata.notDefined());
  }

  private void givenNoMetadata(String rootNodeId) {
    tested = new MetadataConverter(emptyFragmentEvent(),
        TaskMetadata.noMetadata(TASK_NAME, rootNodeId));
  }

  private void givenNodesMetadata(String rootNodeId, NodeMetadata... nodes) {
    givenEventLogAndNodesMetadata(new EventLog(TASK_NAME), rootNodeId, nodes);
  }

  private void givenEventLogAndNodesMetadata(EventLog eventLog, String rootNodeId,
      NodeMetadata... nodes) {
    Map<String, NodeMetadata> metadata = new HashMap<>();
    for (NodeMetadata node : nodes) {
      metadata.put(node.getNodeId(), node);
    }

    tested = new MetadataConverter(emptyFragmentEvent(eventLog),
        TaskMetadata.create(TASK_NAME, rootNodeId, metadata));
  }

  private TaskResult emptyFragmentEvent() {
    return emptyFragmentEvent(new EventLog(TASK_NAME));
  }

  private TaskResult emptyFragmentEvent(EventLog eventLog) {
    TaskResult output = new TaskResult(TASK_NAME, new Fragment("dummy", new JsonObject(), ""));
    output.appendLog(eventLog);
    return output;
  }

  private NodeMetadata singleNode(String id, String factory) {
    return singleNode(id, factory, ImmutableMap.of());
  }

  private NodeMetadata singleNode(String id, String factory, Map<String, String> transitions) {
    return NodeMetadata.single(
        id,
        "simple",
        transitions,
        operationMetadata(factory)
    );
  }

  private OperationMetadata operationMetadata(String factory) {
    final OperationMetadata result;
    if ("action".equals(factory)) {
      result = new OperationMetadata(factory, new JsonObject().put("actionFactory", "http"));
    } else {
      result = new OperationMetadata(factory);
    }
    return result;
  }

  private NodeMetadata compositeSubtasksNode(String id, Map<String, String> transitions,
      String... nested) {
    return compositeNode(id, "subtasks", transitions, nested);
  }

  private NodeMetadata compositeNode(String id, String factory, String... nested) {
    return compositeNode(id, factory, ImmutableMap.of(), nested);
  }

  private NodeMetadata compositeNode(String id, String factory, Map<String, String> transitions,
      String... nested) {
    return NodeMetadata.composite(
        id,
        "composite",
        transitions,
        Arrays.asList(nested),
        operationMetadata(factory)
    );
  }

  private JsonObject jsonForMissingNode(String id) {
    return new JsonObject()
        .put("id", id)
        .put("label", "!")
        .put("type", NodeType.SINGLE)
        .put("status", LoggedNodeStatus.MISSING);
  }

  private JsonObject jsonForNode(String id, String factory) {
    return new JsonObject()
        .put("id", id)
        .put("label", "simple")
        .put("type", NodeType.SINGLE)
        .put("on", new JsonObject())
        .put("subtasks", new JsonArray())
        .put("operation", GraphNodeOperationLog.newInstance(factory, new JsonObject()).toJson())
        .put("status", LoggedNodeStatus.UNPROCESSED);
  }

  private JsonObject jsonForActionNode(String id) {
    return jsonForNode(id, "action")
        .put("operation",
            GraphNodeOperationLog
                .newInstance("action", new JsonObject().put("actionFactory", "http"))
                .toJson());
  }

  private JsonObject jsonForNotDescribedNode(String id) {
    return new JsonObject()
        .put("id", id)
        .put("status", LoggedNodeStatus.UNPROCESSED);
  }

  private JsonArray wrap(JsonObject instance) {
    return new JsonArray(Collections.singletonList(instance));
  }

  private JsonObject simpleNodeLog() {
    return new JsonObject()
        .put("alias", "alias")
        .put("started", 123123213)
        .put("finished", 21342342)
        .put("operation", "http")
        .put("logs", new JsonObject());
  }

  private JsonObject complexNodeLog() {
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

  private CompositeException compositeError() {
    return new CompositeException(
        new IllegalArgumentException("error message 1"),
        new IllegalStateException("error message 2"));
  }
}
