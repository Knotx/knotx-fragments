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
import static io.knotx.fragments.engine.Nodes.composite;
import static io.knotx.fragments.engine.Nodes.single;
import static io.knotx.fragments.engine.TestFunction.appendBody;
import static io.knotx.fragments.engine.TestFunction.appendBodyWithPayload;
import static io.knotx.fragments.engine.TestFunction.appendPayload;
import static io.knotx.fragments.engine.TestFunction.appendPayloadBasingOnContext;
import static io.knotx.fragments.engine.TestFunction.failure;
import static io.knotx.fragments.engine.TestFunction.success;
import static io.knotx.fragments.engine.TestFunction.successWithDelay;
import static io.knotx.fragments.engine.Transitions.onError;
import static io.knotx.fragments.engine.Transitions.onSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.engine.api.node.Node;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TaskEngineScenariosTest {

  private static final String COMPOSITE_NODE_ID = "composite";
  private static final String INITIAL_BODY = "initial body";

  private FragmentEventContext eventContext;
  private Fragment initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);

  @BeforeEach
  void setUp() {
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());
  }

  /*
   * scenario: first -> parallel[A,B,C] -> last
   */
  @Test
  @DisplayName("Expect success status and fragment's body update when parallel processing")
  void expectSuccessParallelProcessing(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    JsonObject taskAPayload = new JsonObject().put("key", "taskAOperation");
    JsonObject taskBPayload = new JsonObject().put("key", "taskBOperation");
    JsonObject taskCPayload = new JsonObject().put("key", "taskCOperation");

    Node rootNode = single("first", appendBody(":first"), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", appendPayload("A", taskAPayload)),
                single("B", appendPayload("B", taskBPayload)),
                single("C", appendPayload("C", taskCPayload))),
            single("last", appendBody(":last")))));
    String expectedBody = INITIAL_BODY + ":first:last";

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          final Fragment fragment = fragmentEvent.getFragment();
          assertEquals(expectedBody, fragment.getBody());
          final JsonObject payload = fragment.getPayload();
          assertEquals(taskAPayload, payload.getJsonObject("A"));
          assertEquals(taskBPayload, payload.getJsonObject("B"));
          assertEquals(taskCPayload, payload.getJsonObject("C"));
        });
  }

  /*
   * scenario: first -> parallel[A, B] -> middle -> parallel[X, Y] -> last
   * X uses payload from A,
   * Y uses payload from B,
   * last uses payload from X and Y to append body
   */
  @Test
  @DisplayName("Expect body updated after complex processing")
  void expectSuccessMultipleParallel(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", appendPayload("A", ":payloadA")),
                single("B", appendPayload("B", ":payloadB"))),
            single("middle", success(), onSuccess(
                composite(COMPOSITE_NODE_ID,
                    parallel(
                        single("X", appendPayloadBasingOnContext("A", "X", "withX")),
                        single("Y", appendPayloadBasingOnContext("B", "Y", "withY"))),
                    single("last", appendBodyWithPayload("X", "Y"))))))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);
    String expectedBody = INITIAL_BODY + ":payloadAwithX:payloadBwithY";

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
          final Fragment fragment = fragmentEvent.getFragment();
          assertEquals(expectedBody, fragment.getBody());
        });
  }

  /*
   * scenario: scenario: first -> parallel[A1 -> A2 -error-> A3(fallback), B] -> middle -> parallel[X, Y1 -> Y2] -> last
   */
  @Test
  @DisplayName("Expect logs in order after complex processing")
  void expectProcessingLogsInOrder(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(),
        onSuccess(
            composite(COMPOSITE_NODE_ID,
                parallel(
                    single("A1", success(), onSuccess(
                        single("A2", failure(), onError(
                            single("A3-fallback", success()))))),
                    single("B", success())),
                single("middle", success(), onSuccess(
                    composite(COMPOSITE_NODE_ID,
                        parallel(
                            single("X", success()),
                            single("Y1", success(), onSuccess(
                                single("Y2", success())))),
                        single("last", success())))))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
          verifyAllLogEntries(fragmentEvent.getLogAsJson(),
              Operation.exact("task", "first", "SUCCESS", 0),
              Operation.range("task", "A1", "SUCCESS", 1, 4),
              Operation.range("task", "A2", "ERROR", 1, 4),
              Operation.range("task", "A3-fallback", "SUCCESS", 1, 4),
              Operation.range("task", "B", "SUCCESS", 1, 4),
              Operation.exact("task", COMPOSITE_NODE_ID, "SUCCESS", 5),
              Operation.exact("task", "middle", "SUCCESS", 6),
              Operation.range("task", "X", "SUCCESS", 7, 9),
              Operation.range("task", "Y1", "SUCCESS", 7, 9),
              Operation.range("task", "Y2", "SUCCESS", 7, 9),
              Operation.exact("task", COMPOSITE_NODE_ID, "SUCCESS", 10),
              Operation.exact("task", "last", "SUCCESS", 11)
          );
        });
  }

  /*
   * scenario: first -> parallel[A, B modifies body: FATAL, C] -> last
   * FATAL after parallel
   */
  @Test
  @Disabled
  @DisplayName("Expect fatal status when body is modified during parallel processing")
  void ensureBodyImmutableDuringParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // ToDo: TBD if we want to implement it
  }

  /*
   * scenario: first -> parallel[A, B, C] -> last
   *  A, B, C all with 500 ms delay, 1s for parallel section
   */
  @Test
  @DisplayName("Expect parallel nodes processed in parallel when delays")
  void verifyParallelExecution(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", successWithDelay(500)),
                single("B", successWithDelay(500)),
                single("C", successWithDelay(500))),
            single("last", success()))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
        }, 1);
  }

  /*
   * scenario: parallel[A, parallel[B -> B1, C], D] -> last
   *  A, B, C, D all with 500 ms delay, 1s for parallel section
   */
  @Test
  @DisplayName("Expect nested parallel nodes processed in parallel when delays")
  void verifyNestedParallelExecution(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", successWithDelay(500)),
                composite(COMPOSITE_NODE_ID,
                    parallel(
                        single("B", successWithDelay(500), onSuccess(
                            single("B1", appendPayload("B1", "B1Payload")))),
                        single("C", successWithDelay(500)))),
                single("D", successWithDelay(500))),
            single("last", success()))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
          JsonObject payload = fragmentEvent.getFragment().getPayload();
          assertEquals("B1Payload", payload.getString("B1"));
        }, 1);
  }

  private List<Node> parallel(Node... nodes) {
    return Arrays.asList(nodes);
  }

  private void verifyExecution(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<FragmentEvent> successConsumer) throws Throwable {
    verifyExecution(result, testContext, successConsumer, 5);
  }

  private void verifyExecution(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<FragmentEvent> successConsumer, int completionTimeout) throws Throwable {
    // execute
    // verifyAllLogEntries
    result.subscribe(
        onSuccess -> testContext.verify(() -> {
          successConsumer.accept(onSuccess);
          testContext.completeNow();
        }), testContext::failNow);

    assertTrue(testContext.awaitCompletion(completionTimeout, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}
