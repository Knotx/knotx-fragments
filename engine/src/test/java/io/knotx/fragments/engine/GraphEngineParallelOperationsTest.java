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
import static io.knotx.fragments.engine.helpers.TestFunction.appendPayload;
import static io.knotx.fragments.engine.helpers.TestFunction.failure;
import static io.knotx.fragments.engine.helpers.TestFunction.fatal;
import static io.knotx.fragments.engine.helpers.TestFunction.success;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.ParallelOperationsNode;
import io.knotx.fragments.engine.graph.SingleOperationNode;
import io.knotx.fragments.handler.api.exception.KnotProcessingFatalException;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class GraphEngineParallelOperationsTest {

  private static final String INITIAL_BODY = "initial body";
  private FragmentEventContext eventContext;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);

  @BeforeEach
  void setUp() {
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());
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
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("action", success(),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("action", success(),
                Collections.emptyMap())
        ),
        null,
        null
    );
    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("action", failure(), Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.FAILURE, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect unsupported event log entries when error transition not handled")
  void expectUnsupportedEventLogEntryWhenError(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("action", failure(), Collections.emptyMap())
        ),
        null,
        null
    );
    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(),
            Operation.of("task", "action", "ERROR"),
            Operation.of("task", "action", "UNSUPPORTED_TRANSITION"),
            Operation.of("task", "parallel", "UNSUPPORTED_TRANSITION")
        ));
  }

  @Test
  @DisplayName("Expect fatal when single parallel action throws fatal")
  void expectExceptionWhenSingleProcessingThrowsFatal(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("action", fatal(eventContext.getFragmentEvent().getFragment()),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("A", appendPayload("A", taskAPayload),
                Collections.emptyMap())
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(taskAPayload,
            fragmentEvent.getFragment().getPayload().getJsonObject("A")));
  }


  @Test
  @DisplayName("Expect success status when parallel inside parallel ends successfully")
  void inception(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new ParallelOperationsNode(
                parallel(
                    new SingleOperationNode("action", success(),
                        Collections.emptyMap())
                ),
                null,
                null
            )
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
                    new SingleOperationNode("action", appendPayload("A", taskAPayload),
                        Collections.emptyMap())
                ),
                null,
                null
            )
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("action", success(),
                Collections.emptyMap())
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("failing", failure(), Collections.emptyMap()),
            new SingleOperationNode("success", success(),
                Collections.emptyMap())
        ), null,
        null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("failing", failure(), Collections.emptyMap()),
            new SingleOperationNode("success", success(),
                Collections.emptyMap())
        ), null, null);

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        event -> verifyLogEntries(event.getLogAsJson(),
            //FixMe parallel section unordered logs
            Operation.of("task", "success", "SUCCESS"),
            Operation.of("task", "failing", "ERROR"),
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
            new SingleOperationNode("A", success(), Collections.emptyMap()),
            new SingleOperationNode("B", failure(), Collections.emptyMap())
        ),
        null,
        new SingleOperationNode("fallback", success(), Collections.emptyMap())
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  @Test
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by action fallback")
  void expectFallbackAppliedDuringParallelProcessing(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    Node rootNode = new ParallelOperationsNode(
        parallel(
            new SingleOperationNode("A", success(), Collections.emptyMap()),
            new SingleOperationNode("B", failure(), Collections.singletonMap(
                ERROR_TRANSITION, new SingleOperationNode("fallback", success(),
                    Collections.emptyMap())
            ))
        ),
        null,
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
            new SingleOperationNode("A", success(), Collections.emptyMap()),
            new SingleOperationNode("B", success(), Collections.emptyMap())
        ),
        new SingleOperationNode("last", appendBody(":last"), Collections.emptyMap()),
        null
    );

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(INITIAL_BODY + ":last",
            fragmentEvent.getFragment().getBody()));
  }

  private List<Node> parallel(Node... nodes) {
    return Arrays.asList(nodes);
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