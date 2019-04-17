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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
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
class GraphEngineScenariosTest {

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

    Node rootNode = new SingleOperationNode("first", appendBody(":first"),
        Collections.singletonMap(DEFAULT_TRANSITION, new ParallelOperationsNode(
                parallel(
                    new SingleOperationNode("A", appendPayload("A", taskAPayload),
                        Collections.emptyMap()),
                    new SingleOperationNode("B", appendPayload("B", taskBPayload),
                        Collections.emptyMap()),
                    new SingleOperationNode("C", appendPayload("C", taskCPayload),
                        Collections.emptyMap())
                ),
                new SingleOperationNode("last", appendBody(":last"), Collections.emptyMap()),
                null
            )
        ));
    String expectedBody = INITIAL_BODY + ":first:last";

    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

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
    Node rootNode = new SingleOperationNode("first", success(),
        Collections.singletonMap(DEFAULT_TRANSITION, new ParallelOperationsNode(
                parallel(
                    new SingleOperationNode("A", appendPayload("A", ":payloadA"),
                        Collections.emptyMap()),
                    new SingleOperationNode("B", appendPayload("B", ":payloadB"),
                        Collections.emptyMap())
                ),
                new SingleOperationNode("middle", success(),
                    Collections.singletonMap("_next", new ParallelOperationsNode(
                        parallel(
                            new SingleOperationNode("X",
                                appendPayloadBasingOnContext("A", "X", "withX"),
                                Collections.emptyMap()),
                            new SingleOperationNode("Y",
                                appendPayloadBasingOnContext("B", "Y", "withY"),
                                Collections.emptyMap())
                        ),
                        new SingleOperationNode("last", appendBodyWithPayload("X", "Y"),
                            Collections.emptyMap()),
                        null
                    ))),
                null
            )
        ));
    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);
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
   * scenario: first -> parallel[A, B modifies body: FATAL, C] -> last
   * FATAL after parallel
   */
  @Test
  @Disabled
  @DisplayName("Expect fatal status when body is modified during parallel processing")
  void ensureBodyImmutableDuringParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // ToDo
  }

  /*
   * scenario: first -> parallel[A, B, C] -> last
   *  A, B, C all with 500 ms delay, 1s for parallel section
   */
  @Test
  @DisplayName("Expect parallel nodes processed in parallel when delays")
  void verifyParallelExecution(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = new SingleOperationNode("first", success(),
        Collections.singletonMap(DEFAULT_TRANSITION, new ParallelOperationsNode(
                parallel(
                    new SingleOperationNode("A", successWithDelay(500),
                        Collections.emptyMap()),
                    new SingleOperationNode("B", successWithDelay(500),
                        Collections.emptyMap()),
                    new SingleOperationNode("C", successWithDelay(500),
                        Collections.emptyMap())
                ),
                new SingleOperationNode("last", success(), Collections.emptyMap()),
                null
            )
        ));
    // when
    Single<FragmentEvent> result = new GraphEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
        }, 1);
  }

  interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  }

  private TestFunction success() {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

  private TestFunction successWithDelay(long delayInMs) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result).delay(delayInMs, TimeUnit.MILLISECONDS);
    };
  }

  private TestFunction appendPayload(String payloadKey, String payloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
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

  private TestFunction appendPayloadBasingOnContext(String expectedPayloadKey,
      String updatedPayloadKey, String updatedPayloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      String payloadValue = fragment.getPayload().getString(expectedPayloadKey);
      fragment.appendPayload(updatedPayloadKey, payloadValue + updatedPayloadValue);
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

  private TestFunction appendBodyWithPayload(String... expectedPayloadKeys) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      for (String expectedPayloadKey : expectedPayloadKeys) {
        String payloadValue = fragment.getPayload().getString(expectedPayloadKey);
        fragment.setBody(fragment.getBody() + payloadValue);
      }
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
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
    // verifyLogEntries
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