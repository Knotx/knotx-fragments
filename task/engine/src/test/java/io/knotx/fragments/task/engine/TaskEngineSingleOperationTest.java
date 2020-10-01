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
import static io.knotx.fragments.task.engine.TestFunction.error;
import static io.knotx.fragments.task.engine.TestFunction.errorWithNodeLog;
import static io.knotx.fragments.task.engine.TestFunction.failure;
import static io.knotx.fragments.task.engine.TestFunction.fatal;
import static io.knotx.fragments.task.engine.TestFunction.success;
import static io.knotx.fragments.task.engine.TestFunction.successWithNodeLog;
import static io.knotx.fragments.task.engine.Transitions.on;
import static io.knotx.fragments.task.engine.Transitions.onError;
import static io.knotx.fragments.task.engine.Transitions.onSuccess;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentOperation;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.api.SyncFragmentOperation;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeFatalException;
import io.knotx.fragments.task.api.single.SingleNode;
import io.knotx.fragments.task.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.task.engine.TaskResult.Status;
import io.knotx.junit5.util.RequestUtil;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Consumer;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
class TaskEngineSingleOperationTest {

  private static final String INITIAL_BODY = "initial body";

  private FragmentContext eventContext;
  private Fragment initialFragment;

  @BeforeEach
  void setUp() {
    initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);
    eventContext = new FragmentContext(initialFragment, new ClientRequest());
  }

  @Test
  @DisplayName("Expect initial fragment when engine operation throws exception.")
  void expectInitialFragment(VertxTestContext testContext, Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", failure());

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(initialFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect new fragment body when operation ends.")
  void expectNewFragmentBodyWhenOperationEnds(VertxTestContext testContext, Vertx vertx) {
    // given
    Node rootNode = Nodes.single("first", appendBody(":updated"));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(new Fragment(initialFragment.toJson())
            .setBody(INITIAL_BODY + ":updated"), event.getFragment()));
  }

  @Test
  @DisplayName("Expect new fragment payload when operation ends.")
  void expectNewFragmentPayloadWhenOperationEnds(VertxTestContext testContext, Vertx vertx) {
    // given
    Node rootNode = Nodes.single("first", appendPayload("newKey", "newValue"));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(new Fragment(initialFragment.toJson())
            .setBody(INITIAL_BODY).appendPayload("newKey", "newValue"), event.getFragment()));
  }

  @Test
  @DisplayName("Expect updated fragment is passed to next operations.")
  void expectAllNodeExecuted(VertxTestContext testContext, Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", appendBody(":A"), onSuccess(
        Nodes.single("second", appendBody(":B"))));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> {
          String body = event.getFragment().getBody();
          assertEquals(INITIAL_BODY + ":A:B", body);
        });
  }

  @Test
  @DisplayName("Expect success status when operation ends.")
  void expectSuccessEventWhenOperationEnds(VertxTestContext testContext, Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", success());

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> Assertions.assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success log entry when operation ends.")
  void expectSuccessLogEntryWhenOperationEnds(VertxTestContext testContext, Vertx vertx) {
    // given
    JsonObject nodeLog = new JsonObject().put("debug", "success");
    SingleNode rootNode = Nodes.single("first", successWithNodeLog(nodeLog));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLog().getOperations(),
            Operation.exact("task", "first", "UNPROCESSED", 0),
            Operation.exact("task", "first", "SUCCESS", 1, nodeLog)
        ));
  }

  @Test
  @DisplayName("Expect success status when all operations ends.")
  void expectSuccessEventWhenAllOperationsEnd(VertxTestContext testContext, Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", success(), onSuccess(
        Nodes.single("second", success())));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation ends with error transition.")
  void expectFailureEventWhenOperationEndsWithErrorTransition(VertxTestContext testContext,
      Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", error());

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect error and unsupported event log entries when operation ends with error transition.")
  void expectErrorAndUnsupportedLogEntriesWhenOperationEndsWithErrorTransition(
      VertxTestContext testContext, Vertx vertx) {
    // given
    JsonObject nodeLog = new JsonObject()
        .put("error", IllegalArgumentException.class.getCanonicalName());
    SingleNode rootNode = Nodes.single("first", errorWithNodeLog(nodeLog));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLog().getOperations(),
            Operation.exact("task", "first", "UNPROCESSED", 0),
            Operation.exact("task", "first", "ERROR", 1, nodeLog),
            Operation.exact("task", "first", "UNSUPPORTED_TRANSITION", 2)
        ));
  }

  @Test
  @DisplayName("Expect success status when operation ends with error transition, next operation ends.")
  void expectSuccessEventWhenOperationEndsWithErrorTransitionNextOperationEnds(
      VertxTestContext testContext, Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", error(), onError(
        Nodes.single("second", success())));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation ends with custom transition that is NOT handled.")
  void expectFailureEventWhenOperationEndsWithCustomTransition(VertxTestContext testContext,
      Vertx vertx) {
    // given
    FragmentOperation operation = (SyncFragmentOperation) context -> FragmentResult
        .success(context.getFragment(), "customTransition");
    SingleNode rootNode = Nodes.single("first", operation);

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }


  @Test
  @DisplayName("Expect success and unsupported event log entries when operation ends with custom transition.")
  void expectSuccessAndUnsupportedEventLogEntryWhenOperationEndsWithCustomTransition(
      VertxTestContext testContext, Vertx vertx) {
    // given
    FragmentOperation operation = (SyncFragmentOperation) context -> FragmentResult
        .success(context.getFragment(), "customTransition");
    SingleNode rootNode = Nodes.single("first", operation);

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLog().getOperations(),
            Operation.exact("task", "first", "UNPROCESSED", 0),
            Operation.exact("task", "first", "SUCCESS", 1),
            Operation.exact("task", "first", "UNSUPPORTED_TRANSITION", 2)
        ));
  }

  @Test
  @DisplayName("Expect success status when operation ends with error transition, next operation ends.")
  void expectSuccessEventWhenOperationEndsWithCustomTransitionNextOperationEnds(
      VertxTestContext testContext, Vertx vertx) {
    // given
    FragmentOperation operation = (SyncFragmentOperation) context -> FragmentResult
        .success(context.getFragment(), "customTransition");
    SingleNode rootNode = Nodes.single("first", operation,
        on("customTransition", Nodes.single("second", success())));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation throws exception.")
  void expectFailureEventWhenOperationThrowsNonFatalException(VertxTestContext testContext,
      Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", failure());

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect error and unsupported transition event logs entries when operation throws exception.")
  void expectErrorAndUnsupportedLogEntriesWhenOperationThrowsNonFatalException(
      VertxTestContext testContext, Vertx vertx) {
    // given
    Throwable error = new IllegalArgumentException("Some message");
    SingleNode rootNode = Nodes.single("first", failure(error));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLog().getOperations(),
            Operation.exact("task", "first", "UNPROCESSED", 0),
            Operation.exact("task", "first", "ERROR", 1, error),
            Operation.exact("task", "first", "UNSUPPORTED_TRANSITION", 2)
        ));
  }

  @Test
  @DisplayName("Expect failure status when operation throws exception.")
  void expectFailureEventWhenOperationThrowsFatalException(VertxTestContext testContext,
      Vertx vertx) {
    // given
    NodeFatalException nodeFatalException = new NodeFatalException("Node fatal exception!");
    SingleNode rootNode = Nodes.single("first", fatal(nodeFatalException));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyFailure(result, testContext, error -> {
      assertTrue(error instanceof CompositeException);
      CompositeException composite = (CompositeException) error;
      assertEquals(2, composite.getExceptions().size());
      assertEquals(NodeFatalException.class, composite.getExceptions().get(0).getClass());
      assertEquals(TaskFatalException.class, composite.getExceptions().get(1).getClass());
      TaskResult event = ((TaskFatalException) composite.getExceptions().get(1))
          .getEvent();
      EventLog log = event.getLog();
      assertEquals(2, log.getOperations().size());
      FragmentEventLogVerifier.verifyAllLogEntries(log.getOperations(),
          Operation.exact("task", "first", "UNPROCESSED", 0),
          Operation.exact("task", "first", "ERROR", 1, nodeFatalException)
      );
    });
  }

  @Test
  @DisplayName("Expect success status when operation throws non-fatal exception, next operation ends.")
  void expectSuccessEventWhenOperationThrowsNonFatalExceptionNextOperationEnds(
      VertxTestContext testContext, Vertx vertx) {
    // given
    SingleNode rootNode = Nodes.single("first", failure(), onError(
        Nodes.single("second", success())));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect error and success event log entries when error transition handled.")
  void expectErrorAndSuccessEventLogEntries(VertxTestContext testContext, Vertx vertx) {
    // given
    Exception nodeException = new IllegalArgumentException("Some node exception!");
    SingleNode rootNode = Nodes.single("first", failure(nodeException), onError(
        Nodes.single("second", success())));

    // when
    Single<TaskResult> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLog().getOperations(),
            Operation.exact("task", "first", "UNPROCESSED", 0),
            Operation.exact("task", "first", "ERROR", 1, nodeException),
            Operation.exact("task", "second", "UNPROCESSED", 2),
            Operation.exact("task", "second", "SUCCESS", 3)
        ));
  }

  private void verifyExecution(Single<TaskResult> result, VertxTestContext testContext,
      Consumer<TaskResult> successConsumer) {
    RequestUtil.subscribeToResult_shouldSucceed(testContext, result, successConsumer);
  }

  private void verifyFailure(Single<TaskResult> result, VertxTestContext testContext,
      Consumer<Throwable> errorConsumer) {
    RequestUtil.subscribeToResult_shouldFail(testContext, result, errorConsumer);
  }
}
