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
package io.knotx.fragments.task.engine;

import static io.knotx.fragments.task.engine.TestFunction.appendBody;
import static io.knotx.fragments.task.engine.TestFunction.appendPayload;
import static io.knotx.fragments.task.engine.TestFunction.failure;
import static io.knotx.fragments.task.engine.TestFunction.fatal;
import static io.knotx.fragments.task.engine.TestFunction.success;
import static io.knotx.fragments.task.engine.TestFunction.successWithNodeLog;
import static io.knotx.fragments.task.engine.Transitions.onError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeFatalException;
import io.knotx.fragments.task.engine.FragmentEvent.Status;
import io.knotx.fragments.task.engine.FragmentEventLogVerifier.Operation;
import io.knotx.junit5.util.RequestUtil;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Consumer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TaskEngineCompositeNodeTest {

  private static final String COMPOSITE_NODE_ID = "composite";
  private static final String INNER_COMPOSITE_NODE_ID = "innerComposite";
  private static final String INITIAL_BODY = "initial body";

  private FragmentEventContext eventContext;

  @BeforeEach
  void setUp() {
    Fragment initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());
  }

  @Test
  @DisplayName("Expect unprocessed status when empty parallel action processing ends")
  void expectUnprocessedWhenEmptyParallelEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID, Collections.emptyList());

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> Assertions.assertEquals(Status.UNPROCESSED, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when single parallel action processing ends")
  void expectSuccessWhenSingleProcessingEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("action", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success parallel event log entry when parallel action processing ends")
  void expectSuccessEventLogEntry(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    JsonObject successNodeLog = new JsonObject().put("debug", "success");
    JsonObject successNode2Log = new JsonObject().put("debug", "success2");
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("action", successWithNodeLog(successNodeLog)),
            Nodes.single("action1", success()),
            Nodes.single("action2", successWithNodeLog(successNode2Log))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(
            event.getLog().getOperations(),
            Operation.exact("task", COMPOSITE_NODE_ID, "UNPROCESSED", 0),
            Operation.range("task", "action", "UNPROCESSED", 1, 5),
            Operation.range("task", "action1", "UNPROCESSED", 1, 5),
            Operation.range("task", "action2", "UNPROCESSED", 1, 5),
            Operation.range("task", "action", "SUCCESS", 2, 6, successNodeLog),
            Operation.range("task", "action1", "SUCCESS", 2, 6),
            Operation.range("task", "action2", "SUCCESS", 2, 6, successNode2Log),
            Operation.exact("task", COMPOSITE_NODE_ID, "SUCCESS", 7)
        ));
  }


  @Test
  @DisplayName("Expect failure status when single parallel action processing fails")
  void expectErrorWhenSingleProcessingFails(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("action", failure())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.FAILURE, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect error parallel event log entry when error transition is handled.")
  void expectErrorEventLogEntry(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("action", failure())),
        null,
        Nodes.single("action", success()));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> FragmentEventLogVerifier.verifyLogEntries(fragmentEvent.getLog().getOperations(),
            Operation.exact("task", COMPOSITE_NODE_ID, "ERROR", 4)
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when error transition not handled")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("action", failure())));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLog().getOperations(),
            Operation.exact("task", COMPOSITE_NODE_ID, "UNSUPPORTED_TRANSITION", 5)
        ));
  }

  @Test
  @DisplayName("Expect fatal when single parallel action throws fatal")
  void expectExceptionWhenSingleProcessingThrowsFatal(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    NodeFatalException nodeFatalException = new NodeFatalException("Node fatal exception!");
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("action", fatal(nodeFatalException))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyError(result, testContext,
        error -> {
          assertTrue(error instanceof CompositeException);
          List<Throwable> exceptions = ((CompositeException) error).getExceptions();
          assertTrue(exceptions.stream().anyMatch(NodeFatalException.class::isInstance));
        }
    );
  }

  @Test
  @DisplayName("Expect payload updated when parallel action ends")
  void expectPayloadUpdatedInParallelProcessing(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    JsonObject taskAPayload = new JsonObject().put("key", "taskAOperation");

    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("A", appendPayload("A", taskAPayload))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(taskAPayload,
            fragmentEvent.getFragment().getPayload().getJsonObject("A")));
  }


  @Test
  @DisplayName("Expect success status when parallel inside parallel ends successfully")
  void inception(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.composite(INNER_COMPOSITE_NODE_ID,
                parallel(
                    Nodes.single("action", success())))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect payload updated when parallel inside parallel action ends")
  void expectPayloadUpdatedInParallelInsideParallelProcessing(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given
    JsonObject taskAPayload = new JsonObject().put("key", "taskAOperation");
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.composite(INNER_COMPOSITE_NODE_ID,
                parallel(
                    Nodes.single("action", appendPayload("A", taskAPayload))))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(taskAPayload,
            fragmentEvent.getFragment().getPayload().getJsonObject("A")));
  }

  @Test
  @DisplayName("Expect success when only one of parallel actions ends and another is empty")
  void expectSuccessWhenParallelConsistsOfEmptyAndSuccessActions(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.composite(INNER_COMPOSITE_NODE_ID, parallel()),
            Nodes.single("action", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect error when one of parallel actions ends with error")
  void expectError(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("failing", failure()),
            Nodes.single("success", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.FAILURE, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect all parallel log entries when one of parallel actions ends with error")
  void expectLogEntriesOfAllParallelActions(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Exception nodeException = new IllegalArgumentException("Some node error message");
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("failing", failure(nodeException)),
            Nodes.single("success", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(
            event.getLog().getOperations(),
            Operation.exact("task", COMPOSITE_NODE_ID, "UNPROCESSED", 0),
            Operation.range("task", "success", "UNPROCESSED", 1, 4),
            Operation.range("task", "failing", "UNPROCESSED", 1, 4),
            Operation.range("task", "success", "SUCCESS", 2, 5),
            Operation.range("task", "failing", "ERROR", 2, 5, nodeException),
            Operation.range("task", "failing", "UNSUPPORTED_TRANSITION", 3, 6),
            Operation.exact("task", COMPOSITE_NODE_ID, "ERROR", 6),
            Operation.exact("task", COMPOSITE_NODE_ID, "UNSUPPORTED_TRANSITION", 7)
        ));
  }

  @Test
  @DisplayName("Expect inner composite node log entry")
  void expectNamedInnerCompositeLogEntry(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.composite(INNER_COMPOSITE_NODE_ID,
                parallel(
                    Nodes.single("success", success())))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLog().getOperations(),
            Operation.exact("task", INNER_COMPOSITE_NODE_ID, "SUCCESS", 4)
        ));
  }

  @Test
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by parallel section fallback")
  void expectFallbackAppliedAfterParallelProcessing(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("A", success()),
            Nodes.single("B", failure())),
        null,
        Nodes.single("fallback", success()));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by action fallback")
  void expectFallbackAppliedDuringParallelProcessing(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("A", success()),
            Nodes.single("B", failure(), onError(
                Nodes.single("fallback", success())))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  /*
   * scenario: scenario: parallel[A -error-> , B -error-> A3(fallback) ] -error->
   */
  @Test
  @DisplayName("Expect fallback payload entry when parallel processing returns error and one of parallel actions returns error that is handled by action fallback and ")
  void expectFallbackPayloadWhenParallelProcessingFails(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("A", failure()),
            Nodes.single("B", failure(), onError(
                Nodes.single("fallback", appendPayload("fallback", "value"))))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertTrue(
            fragmentEvent.getFragment().getPayload().containsKey("fallback")));
  }

  @Test
  @DisplayName("Expect success operation applied when parallel processing ends with success")
  void expectSuccessAppliedAfterParallelProcessingSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("A", success()),
            Nodes.single("B", success())),
        Nodes.single("last", appendBody(":last")),
        null);

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(INITIAL_BODY + ":last",
            fragmentEvent.getFragment().getBody()));
  }

  @Test
  @DisplayName("Expect success after composite action applied on parallel processing success")
  void expectSuccessAfterParallelProcessingAppliedAfterSuccessParallel(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given
    JsonObject expectedPayload = new JsonObject().put("key", "value");
    Node rootNode = Nodes.composite(COMPOSITE_NODE_ID,
        parallel(
            Nodes.single("A", success()),
            Nodes.single("B", success())),
        Nodes.composite(COMPOSITE_NODE_ID,
            parallel(
                Nodes.single("last", appendPayload("last", expectedPayload)))),
        null);

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(expectedPayload,
            fragmentEvent.getFragment().getPayload().getJsonObject("last")));
  }

  private List<Node> parallel(Node... nodes) {
    return Arrays.asList(nodes);
  }

  private void verifyExecution(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<FragmentEvent> successConsumer) throws Throwable {
    RequestUtil.subscribeToResult_shouldSucceed(testContext, result, successConsumer);
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private void verifyError(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<Throwable> errorConsumer) throws Throwable {
    RequestUtil.subscribeToResult_shouldFail(testContext, result, errorConsumer);
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}