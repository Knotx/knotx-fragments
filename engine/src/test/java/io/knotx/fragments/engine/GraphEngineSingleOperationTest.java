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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.engine;

import static io.knotx.fragments.engine.FragmentEventLogVerifier.verifyLogEntries;
import static io.knotx.fragments.engine.helpers.TestFunction.appendBody;
import static io.knotx.fragments.engine.helpers.TestFunction.failure;
import static io.knotx.fragments.engine.helpers.TestFunction.success;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleOperationNode;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class GraphEngineSingleOperationTest {

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
    Node rootNode = new SingleOperationNode("first", appendBody(":updated"),
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(evaluatedFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect initial fragment when engine operation throws exception.")
  void expectInitialFragment(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", failure(),
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(initialFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect all graph node operations are executed.")
  void expectrootNodeOperations(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", appendBody(":A"),
        Collections.singletonMap(DEFAULT_TRANSITION,
            new SingleOperationNode("second", appendBody(":B"), Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
    SingleOperationNode rootNode = new SingleOperationNode("first", success(),
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when all operations ends.")
  void expectSuccessEventWhenAllOperationsEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", success(),
        Collections.singletonMap(DEFAULT_TRANSITION,
            new SingleOperationNode("second", success(), Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation throws exception.")
  void expectFailureEventWhenUnhandledException(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", failure(),
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when operation throws exception and error transition is handled.")
  void expectSuccessEventWhenExceptionHandled(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", failure(),
        Collections.singletonMap(ERROR_TRANSITION,
            new SingleOperationNode("second", success(), Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation ends with custom transition that is NOT handled.")
  void executeEventWithInvalidAddressInKnotFlow(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Function<FragmentContext, Single<FragmentResult>> operation = context -> Single
        .just(new FragmentResult(context.getFragment(), "customTransition"));
    SingleOperationNode rootNode = new SingleOperationNode("knotx.knot.successKnot",
        operation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success event log entry when operation ends.")
  void expectSuccessEventLogEntry(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", success(),
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(), Operation.of("task", "first", "SUCCESS")
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when error transition not handled.")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", failure(),
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "first", "ERROR"),
            Operation.of("task", "first", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when custom transition not handled.")
  void expectUnsupportedEventLogEntryWhenCustomTransition(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Function<FragmentContext, Single<FragmentResult>> operation = context -> Single
        .just(new FragmentResult(context.getFragment(), "customTransition"));
    SingleOperationNode rootNode = new SingleOperationNode("first", operation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "first", "SUCCESS"),
            Operation.of("task", "first", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect error and success event log entries when error transition handled.")
  void expectErrorAndSuccessEventLogEntries(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    SingleOperationNode rootNode = new SingleOperationNode("first", failure(),
        Collections.singletonMap(ERROR_TRANSITION,
            new SingleOperationNode("second", success(), Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "first", "ERROR"),
            Operation.of("task", "second", "SUCCESS")
        ));
  }

  private void verifyExecution(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<FragmentEvent> successConsumer) throws Throwable {
    // execute
    // verifyLogEntries
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