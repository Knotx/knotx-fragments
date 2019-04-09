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
import static io.knotx.fragments.handler.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventLogVerifier.Operation;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GraphEngineTest {

  private FragmentEventContext eventContext;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), "some body");
  private Fragment evaluatedFragment = new Fragment(initialFragment.toJson()).setBody("updated body");

  @Mock
  private TestFunction successOperation;

  @Mock
  private TestFunction invalidOperation;

  @BeforeEach
  void setUp() {
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());

    when(successOperation.apply(Mockito.any())).thenReturn(Single.just(
        new FragmentResult(evaluatedFragment, DEFAULT_TRANSITION)));
    when(invalidOperation.apply(Mockito.any())).thenThrow(new RuntimeException());
  }

  @Test
  @DisplayName("Expect evaluated fragment when engine operation ends.")
  void expectEvaluatedFragment(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", successOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(evaluatedFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect initial fragment when engine operation throws exception.")
  void expectInitialFragment(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", invalidOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> assertEquals(initialFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect all graph node operations are executed.")
  void expectGraphNodeOperations(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", successOperation,
        Collections.singletonMap(DEFAULT_TRANSITION,
            Collections.singletonList(
                new GraphNode("taskA", "second", successOperation, Collections.emptyMap()))));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> verify(successOperation, times(2)).apply(Mockito.any()));
  }

  @Test
  @DisplayName("Expect success status when operation ends.")
  void expectSuccessEventWhenOperationEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", successOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when all operations ends.")
  void expectSuccessEventWhenAllOperationsEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", successOperation,
        Collections.singletonMap(DEFAULT_TRANSITION,
            Collections.singletonList(
                new GraphNode("taskA", "second", successOperation, Collections.emptyMap()))));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation throws exception.")
  void expectFailureEventWhenUnhandledException(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", invalidOperation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when operation throws exception and error transition is handled.")
  void expectSuccessEventWhenExceptionHandled(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", invalidOperation,
        Collections.singletonMap(ERROR_TRANSITION,
            Collections.singletonList(
                new GraphNode("taskA", "second", successOperation, Collections.emptyMap()))));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

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
    GraphNode graphNode = new GraphNode("taskA", "knotx.knot.successKnot", operation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success event log entry when operation ends.")
  void expectSuccessEventLogEntry(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", successOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(),
            Operation.of("taskA", "first", "SUCCESS")
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when error transition not handled.")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", invalidOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLogAsJson(),
            Operation.of("taskA", "first", "ERROR"),
            Operation.of("taskA", "first", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when custom transition not handled.")
  void expectUnsupportedEventLogEntryWhenCustomTransition(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Function<FragmentContext, Single<FragmentResult>> operation = context -> Single
        .just(new FragmentResult(context.getFragment(), "customTransition"));
    GraphNode graphNode = new GraphNode("taskA", "first", operation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLogAsJson(),
            Operation.of("taskA", "first", "SUCCESS"),
            Operation.of("taskA", "first", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect error and success event log entries when error transition handled.")
  void expectErrorAndSuccessEventLogEntries(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("taskA", "first", invalidOperation,
        Collections.singletonMap(ERROR_TRANSITION,
            Collections.singletonList(
                new GraphNode("taskA", "second", successOperation, Collections.emptyMap()))));

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> FragmentEventLogVerifier.verifyLogEntries(event.getLogAsJson(),
            Operation.of("taskA", "first", "ERROR"),
            Operation.of("taskA", "second", "SUCCESS")
        ));
  }

  @Test
  @DisplayName("Expect success status and fragment's body update when parallel processing")
  void expectSuccessParallelProcessing() {
    // scenario:
    // first -> parallel[A,B,C] -> last
    // body expected
    // payload expected
  }

  @Test
  @DisplayName("Expect error status when parallel processing and one of parallel actions returns error")
  void expectErrorParallelProcessing() {
    // scenario:
    // first -> parallel[A, B with ERROR, C] -> last
    // error after parallel
  }

  @Test
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by parallel section fallback")
  void expectFallbackAppliedAfterParallelProcessing() {
    // scenario:
    // first -> parallel[A, B with ERROR, C] -> errorFallback
    // error after parallel but handled by fallback
  }

  @Test
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by action fallback")
  void expectFallbackAppliedDuringParallelProcessing() {
    // scenario:
    // first -> parallel[A, B with ERROR -> fallbackB, C] -> last
    // error at parallel B but handled by fallbackB
  }

  @Test
  @DisplayName("Expect success status when nested parallel processing")
  void expectSuccessNestedParallel() {
    // scenario:
    // first -> parallel[A, parallel[B', B'']] -> last
  }

  @Test
  @DisplayName("Expect success status when nested parallel processing")
  void expectSuccessMultipleParallel() {
    // scenario:
    // first -> parallel[A, B] -> middle -> parallel[X, Y] -> last
  }


  @Test
  @DisplayName("Expect success status when processing starts in parallel")
  void startWithParallel() {
    // scenario:
    // parallel[A, B, C] -> last
  }


  @Test
  @DisplayName("Expect success status when processing ends in parallel")
  void endWithParallel() {
    // scenario:
    // first -> parallel[A, B, C]
  }

  @Test
  @DisplayName("Expect fatal status when body is modified during parallel processing")
  void ensureBodyImmutableDuringParallelProcessing() {
    // scenario:
    // first -> parallel[A, B modifies body: FATAL, C] -> last
    // FATAL after parallel
  }

  @Test
  @DisplayName("Expect modified body in the step after parallel when it was modified before parallel")
  void ensureBodyModifiedBeforeParallelProcessingIsPassedAfter() {
    // scenario:
    // first (modify body) -> parallel[A, B, C] -> last
    // modified body passed to last
  }

  @Test
  @DisplayName("Expect parallel nodes when processed in parallel")
  void verifyParallelExecution() {
    // scenario:
    // first -> parallel[A, B, C] -> last
    // A, B, C all with 500 ms delay, 1s for parallel section
  }

  @Test
  @DisplayName("Expect success nodes when processed in parallel and data from parallel is required by subsequent step")
  void verifyDataFlowInParallelExecution() {
    // scenario:
    // first -> parallel[A, B -> B1 -> B2, C] -> last
    // B2 uses data from B
    // last uses data from A, B2, C
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

  interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  }

}