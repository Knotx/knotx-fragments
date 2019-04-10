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

import static io.knotx.fragments.handler.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.ParallelOperationsNode;
import io.knotx.fragments.engine.graph.SingleOperationNode;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
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
class GraphEngineParallelOperationsTest {

  private FragmentEventContext eventContext;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), "some body");
  private Fragment evaluatedFragment = new Fragment(initialFragment.toJson())
      .setBody("updated body");

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

  /*
   * scenario: first -> parallel[A,B,C] -> last
   */
  @Test
  @DisplayName("Expect success status and fragment's body update when parallel processing")
  void expectSuccessParallelProcessing(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = new SingleOperationNode("taskA", "first", successOperation,
        Collections.singletonMap(DEFAULT_TRANSITION, new ParallelOperationsNode(
              parallel(
                  new SingleOperationNode("taskA", "A", successOperation, Collections.emptyMap()),
                  new SingleOperationNode("taskB", "B", successOperation, Collections.emptyMap()),
                  new SingleOperationNode("taskC", "C", successOperation, Collections.emptyMap())
              ), new SingleOperationNode("lastTask", "last", successOperation, Collections.emptyMap()), null
            )
            ));


    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start(eventContext, rootNode);

    // then
    // ToDo check body and payload
    verifyExecution(result, testContext,
        event -> assertEquals(evaluatedFragment, event.getFragment()));
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect error status when parallel processing and one of parallel actions returns error")
  void expectErrorParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B with ERROR, C] -> last
    // error after parallel
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by parallel section fallback")
  void expectFallbackAppliedAfterParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B with ERROR, C] -> errorFallback
    // error after parallel but handled by fallback
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success status when parallel processing and one of parallel actions returns error that is handled by action fallback")
  void expectFallbackAppliedDuringParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B with ERROR -> fallbackB, C] -> last
    // error at parallel B but handled by fallbackB
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success status when nested parallel processing")
  void expectSuccessNestedParallel(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, parallel[B', B'']] -> last
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success status when nested parallel processing")
  void expectSuccessMultipleParallel(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B] -> middle -> parallel[X, Y] -> last
  }


  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success status when processing starts in parallel")
  void startWithParallel(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // parallel[A, B, C] -> last
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success status when processing ends in parallel")
  void endWithParallel(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B, C]
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect fatal status when body is modified during parallel processing")
  void ensureBodyImmutableDuringParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B modifies body: FATAL, C] -> last
    // FATAL after parallel
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect modified body in the step after parallel when it was modified before parallel")
  void ensureBodyModifiedBeforeParallelProcessingIsPassedAfter(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first (modify body) -> parallel[A, B, C] -> last
    // modified body passed to last
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect parallel nodes when processed in parallel")
  void verifyParallelExecution(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B, C] -> last
    // A, B, C all with 500 ms delay, 1s for parallel section
  }

  /*
   * scenario:
   */
  @Test
  @Disabled
  @DisplayName("Expect success nodes when processed in parallel and data from parallel is required by subsequent step")
  void verifyDataFlowInParallelExecution(VertxTestContext testContext, Vertx vertx) {
    // scenario:
    // first -> parallel[A, B -> B1 -> B2, C] -> last
    // B2 uses data from B
    // last uses data from A, B2, C
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

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  }

}