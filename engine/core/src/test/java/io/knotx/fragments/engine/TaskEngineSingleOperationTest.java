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

import static io.knotx.fragments.engine.FragmentEventLogVerifier.verifyAllLogEntries;
import static io.knotx.fragments.engine.Nodes.single;
import static io.knotx.fragments.engine.TestFunction.appendBody;
import static io.knotx.fragments.engine.TestFunction.errorWithNodeLog;
import static io.knotx.fragments.engine.TestFunction.failure;
import static io.knotx.fragments.engine.TestFunction.success;
import static io.knotx.fragments.engine.TestFunction.successWithNodeLog;
import static io.knotx.fragments.engine.Transitions.onError;
import static io.knotx.fragments.engine.Transitions.onSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentOperation;
import io.knotx.fragments.engine.api.FragmentEvent;
import io.knotx.fragments.engine.api.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.engine.api.FragmentEventContext;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.single.SingleNode;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TaskEngineSingleOperationTest {

  private static final String INITIAL_BODY = "initial body";

  private FragmentEventContext eventContext;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);
  private Fragment evaluatedFragment = new Fragment(initialFragment.toJson())
      .setBody("initial body:updated");

  @BeforeEach
  void setUp() {
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());
  }

  @Test
  @DisplayName("Expect evaluated fragment when engine operation ends.")
  void expectEvaluatedFragment(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = single("first", appendBody(":updated"));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(evaluatedFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect initial fragment when engine operation throws exception.")
  void expectInitialFragment(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", failure());

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(initialFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect all graph node operations are executed.")
  void expectrootNodeOperations(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", appendBody(":A"), onSuccess(
        single("second", appendBody(":B"))));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> {
          String body = event.getFragment().getBody();
          assertEquals(INITIAL_BODY + ":A:B", body);
        });
  }

  @Test
  @DisplayName("Expect success status when operation ends.")
  void expectSuccessEventWhenOperationEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", success());

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when all operations ends.")
  void expectSuccessEventWhenAllOperationsEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", success(), onSuccess(
        single("second", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation throws exception.")
  void expectFailureEventWhenUnhandledException(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", failure());

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when operation throws exception and error transition is handled.")
  void expectSuccessEventWhenExceptionHandled(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", failure(), onError(
        single("second", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation ends with custom transition that is NOT handled.")
  void executeEventWithInvalidAddressInKnotFlow(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    FragmentOperation operation = (context, handler) -> Future
        .succeededFuture(new FragmentResult(context.getFragment(), "customTransition"))
        .setHandler(handler);
    SingleNode rootNode = single("knotx.knot.successKnot", operation);

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success event log entry when operation ends.")
  void expectSuccessEventLogEntry(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", success());

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> verifyAllLogEntries(event.getLogAsJson(),
            Operation.exact("task", "first", "SUCCESS", 0)
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when error transition not handled.")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", failure());

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLogAsJson(),
            Operation.exact("task", "first", "ERROR", 0),
            Operation.exact("task", "first", "UNSUPPORTED_TRANSITION", 1)
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when custom transition not handled.")
  void expectUnsupportedEventLogEntryWhenCustomTransition(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    FragmentOperation operation = (context, handler) -> Future
        .succeededFuture(new FragmentResult(context.getFragment(), "customTransition"))
        .setHandler(handler);
    SingleNode rootNode = single("first", operation);

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLogAsJson(),
            Operation.exact("task", "first", "SUCCESS", 0),
            Operation.exact("task", "first", "UNSUPPORTED_TRANSITION", 1)
        ));
  }

  @Test
  @DisplayName("Expect error and success event log entries when error transition handled.")
  void expectErrorAndSuccessEventLogEntries(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleNode rootNode = single("first", failure(), onError(
        single("second", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLogAsJson(),
            Operation.exact("task", "first", "ERROR", 0),
            Operation.exact("task", "second", "SUCCESS", 1)
        ));
  }

  @Test
  @DisplayName("Expect node debug in log event log entries when success transition handled.")
  void expectNodeDebugLogEventLogEntriesForSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    JsonObject successNodeLog = new JsonObject().put("debug", "success");
    SingleNode rootNode = single("first", successWithNodeLog(successNodeLog), onSuccess(
        single("second", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLogAsJson(),
            Operation.exact("task", "first", "SUCCESS", 0, successNodeLog),
            Operation.exact("task", "second", "SUCCESS", 1)
        ));
  }

  @Test
  @DisplayName("Expect node log in  event log entries when error transition handled.")
  void expectNodeDebugLogEventLogEntriesForError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    JsonObject errorNodeLog = new JsonObject().put("debug", "error");
    SingleNode rootNode = single("first", errorWithNodeLog(errorNodeLog), onError(
        single("second", success())));

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyAllLogEntries(event.getLogAsJson(),
            Operation.exact("task", "first", "SUCCESS", 0, errorNodeLog),
            Operation.exact("task", "second", "SUCCESS", 1)
        ));
  }

  private void verifyExecution(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<FragmentEvent> successConsumer) throws Throwable {
    // execute
    // verifyAllLogEntries
    result.subscribe(
        onSuccess -> testContext.verify(() -> {
          successConsumer.accept(onSuccess);
          testContext.completeNow();
        }), testContext::failNow);

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}