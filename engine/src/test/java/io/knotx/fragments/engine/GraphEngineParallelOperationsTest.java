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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.api.exception.KnotProcessingFatalException;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
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
class GraphEngineParallelOperationsTest {

  private static final String INITIAL_BODY = "initial body";
  private FragmentEventContext eventContext;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);

  @Mock
  private TestFunction invalidOperation;

  @BeforeEach
  void setUp() {
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());

    when(invalidOperation.apply(Mockito.any())).thenThrow(new RuntimeException());
  }

  @Test
  @DisplayName("Expect unprocessed status when empty parallel action processing ends")
  void expectUnprocessedWhenEmptyParallelEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.UNPROCESSED, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when single parallel action processing ends")
  void expectSuccessWhenSingleProcessingEnds(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "action", success(),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success event log entry when parallel action processing ends")
  void expectSuccessEventLogEntry(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "action", success(), Collections.emptyMap())
        ),
        null,
        null
    );
    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "action", "SUCCESS")
        ));
  }


  @Test
  @DisplayName("Expect failure status when single parallel action processing fails")
  void expectErrorWhenSingleProcessingFails(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "action", failure(),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.FAILURE, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("EExpect unsupported event log entries when error transition not handled")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "action", failure(), Collections.emptyMap())
        ),
        null,
        null
    );
    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "action", "ERROR"),
            Operation.of("task", "action", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect fatal when single parallel action throws fatal")
  void expectExceptionWhenSingleProcessingThrowsFatal(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "action",
                fatal(eventContext.getFragmentEvent().getFragment()),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx)
        .start(eventContext, rootNode);

    // then
    verifyError(result, testContext,
        error -> assertTrue(error.getExceptions().stream()
            .anyMatch(KnotProcessingFatalException.class::isInstance))
    );
  }

  @Test
  @DisplayName("Expect payload updated when parallel action ends")
  void expectPayloadUpdatedInParallelProcessing(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    JsonObject taskAPayload = new JsonObject().put("key", "taskAOperation");

    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("taskA", "A", appendPayload("A", taskAPayload),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(taskAPayload, fragmentEvent.getFragment().getPayload().getJsonObject("A"));
        });
  }


  @Test
  @DisplayName("Expect success status when parallel inside parallel ends successfully")
  void inception(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new ParallelOperationsNode(
                parallel(
                    new SingleOperationNode("task", "action", success(),
                        Collections.emptyMap())
                ),
                null,
                null
            )
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

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
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new ParallelOperationsNode(
                parallel(
                    new SingleOperationNode("task", "action", appendPayload("A", taskAPayload),
                        Collections.emptyMap())
                ),
                null,
                null
            )
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

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
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new ParallelOperationsNode(
                parallel(),
                null,
                null
            ),
            new SingleOperationNode("task", "action", success(),
                Collections.emptyMap())
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect error when one of parallel actions ends with error")
  void expectError(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "failing", failure(),
                Collections.emptyMap()),
            new SingleOperationNode("task", "success", success(),
                Collections.emptyMap())
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.FAILURE, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect all parallel log entries when one of parallel actions ends with error")
  void expectLogEntriesOfAllParallelActions(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "failing", failure(),
                Collections.emptyMap()),
            new SingleOperationNode("task", "success", success(),
                Collections.emptyMap())
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "failing", "ERROR"),
            Operation.of("task", "success", "SUCCESS"),
            // Operation.of("task", "parallel", "UNSUPPORTED_TRANSITION")
            // ToDo: should be the last log UNSUPPORTED_TRANSITION when ERROR in parallel?
            Operation.of("task", "failing", "UNSUPPORTED_TRANSITION"),
            Operation.of("task", "parallel", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by parallel section fallback")
  void expectFallbackAppliedAfterParallelProcessing(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given

    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "A", success(),
                Collections.emptyMap()),
            new SingleOperationNode("task", "B", failure(),
                Collections.emptyMap())
        ),
        null,
        new SingleOperationNode("task", "fallback", success(),
            Collections.emptyMap())
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success operation applied when parallel processing ends with success")
  void expectSuccessAppliedAfterParallelProcessingSuccess(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given

    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("task", "A", success(),
                Collections.emptyMap()),
            new SingleOperationNode("task", "B", success(),
                Collections.emptyMap())
        ),
        new SingleOperationNode("task", "last", appendBody(":last"),
            Collections.emptyMap()),
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(INITIAL_BODY + ":last", fragmentEvent.getFragment().getBody()));
  }

  //ToDo extract to separate enum with instances
  interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  }

  private TestFunction success() {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

  private TestFunction failure() {
    return fragmentContext -> {
      throw new RuntimeException();
    };
  }

  private TestFunction fatal(Fragment fragment) {
    return fragmentContext -> {
      throw new KnotProcessingFatalException(fragment);
    };
  }

  private TestFunction appendPayload(String payloadKey, JsonObject payloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

  private TestFunction appendBody(String postfix) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.setBody(fragment.getBody() + postfix);
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

  private Set<Node> parallel(Node... nodes) {
    return new HashSet<>(Arrays.asList(nodes));
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

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private void verifyError(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<CompositeException> errorConsumer) throws Throwable {
    // execute
    // verifyLogEntries
    result.subscribe(
        onSuccess -> testContext.failNow(new IllegalStateException()),
        onError -> testContext.verify(() -> {
          errorConsumer.accept((CompositeException) onError);
          testContext.completeNow();
        }));

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }


}