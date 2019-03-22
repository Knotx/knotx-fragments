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
package io.knotx.engine.core;

import static io.knotx.engine.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static io.knotx.engine.api.fragment.FragmentResult.ERROR_TRANSITION;
import static io.knotx.engine.core.FlowEntryLogVerifier.verifyLogEntries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEvent.Status;
import io.knotx.engine.api.FragmentEventContext;
import io.knotx.engine.api.fragment.FragmentContext;
import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.engine.core.FlowEntryLogVerifier.Operation;
import io.knotx.fragment.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
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
  private GraphEngine engine;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), "some body");
  private Fragment evaluatedFragment = new Fragment("snippet", new JsonObject(), "updated body");

  @Mock
  private TestFunction successOperation;

  @Mock
  private TestFunction invalidOperation;

  @BeforeEach
  void setUp() {
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());
    engine = new GraphEngine();

    when(successOperation.apply(Mockito.any())).thenReturn(Single.just(
        new FragmentResult(evaluatedFragment, DEFAULT_TRANSITION)));
    when(invalidOperation.apply(Mockito.any())).thenThrow(new RuntimeException());
  }

  @Test
  @DisplayName("Expect evaluated fragment when engine operation ends.")
  void expectEvaluatedFragment(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", successOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> Assertions.assertEquals(evaluatedFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect initial fragment when engine operation throws exception.")
  void expectInitialFragment(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", invalidOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> Assertions.assertEquals(initialFragment, event.getFragment()));
  }

  @Test
  @DisplayName("Expect all graph node operations are executed.")
  void expectGraphNodeOperations(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", successOperation,
        Collections.singletonMap(DEFAULT_TRANSITION,
            new GraphNode("second", successOperation, Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> verify(successOperation, times(2)).apply(Mockito.any()));
  }

  @Test
  @DisplayName("Expect success status when operation ends.")
  void expectSuccessEventWhenOperationEnds(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", successOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when all operations ends.")
  void expectSuccessEventWhenAllOperationsEnds(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", successOperation,
        Collections.singletonMap(DEFAULT_TRANSITION,
            new GraphNode("second", successOperation, Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation throws exception.")
  void expectFailureEventWhenUnhandledException(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", invalidOperation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when operation throws exception and error transition is handled.")
  void expectSuccessEventWhenExceptionHandled(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", invalidOperation,
        Collections.singletonMap(ERROR_TRANSITION,
            new GraphNode("second", successOperation, Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.SUCCESS, event.getStatus()));
  }

  @Test
  @DisplayName("Expect failure status when operation ends with custom transition that is NOT handled.")
  void executeEventWithInvalidAddressInKnotFlow(VertxTestContext testContext)
      throws Throwable {
    // given
    Function<FragmentContext, Single<FragmentResult>> operation = context -> Single
        .just(new FragmentResult(context.getFragment(), "customTransition"));
    GraphNode graphNode = new GraphNode("knotx.knot.successKnot", operation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> assertEquals(Status.FAILURE, event.getStatus()));
  }

  @Test
  @DisplayName("Expect success event log entry when operation ends.")
  void expectSuccessEventLogEntry(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", successOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLog(),
            Operation.of("first", "SUCCESS")
        ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when error transition not handled.")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", invalidOperation, Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> verifyLogEntries(event.getLog(),
        Operation.of("first", "ERROR"),
        Operation.of("first", "UNSUPPORTED_TRANSITION")
    ));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when custom transition not handled.")
  void expectUnsupportedEventLogEntryWhenCustomTransition(VertxTestContext testContext)
      throws Throwable {
    // given
    Function<FragmentContext, Single<FragmentResult>> operation = context -> Single
        .just(new FragmentResult(context.getFragment(), "customTransition"));
    GraphNode graphNode = new GraphNode("first", operation,
        Collections.emptyMap());

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> verifyLogEntries(event.getLog(),
        Operation.of("first", "SUCCESS"),
        Operation.of("first", "UNSUPPORTED_TRANSITION")
    ));
  }

  @Test
  @DisplayName("Expect error and success event log entries when error transition handled.")
  void expectErrorAndSuccessEventLogEntries(VertxTestContext testContext)
      throws Throwable {
    // given
    GraphNode graphNode = new GraphNode("first", invalidOperation,
        Collections.singletonMap(ERROR_TRANSITION,
            new GraphNode("second", successOperation, Collections.emptyMap())));

    // when
    Single<FragmentEvent> result = engine.start(eventContext, graphNode);

    // then
    verifyExecution(result, testContext, event -> verifyLogEntries(event.getLog(),
        Operation.of("first", "ERROR"),
        Operation.of("second", "SUCCESS")
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

  interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  }

}