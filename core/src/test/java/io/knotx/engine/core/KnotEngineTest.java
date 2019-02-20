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

import static io.knotx.engine.core.EntryLogTestHelper.verifyLogEntries;

import com.google.common.collect.Lists;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEvent.Status;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.api.KnotProcessingFatalException;
import io.knotx.engine.api.TraceableKnotOptions;
import io.knotx.engine.core.EntryLogTestHelper.Operation;
import io.knotx.engine.core.impl.KnotEngineFactory;
import io.knotx.fragment.Fragment;
import io.knotx.knotengine.core.junit.MockKnotProxy;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class KnotEngineTest {

  @BeforeEach
  void setUp() {
    Assertions.assertTrue(System.getenv().containsKey("vertx.logger-delegate-factory-class-name"));
  }

  @Test
  @DisplayName("Expect an unprocessed event status when no processing flow is defined")
  void execute_whenEventWithNoKnot_expectEventWithUnprocessedStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    KnotFlow knotFlow = null;

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      Assertions.assertTrue(events.get(0).getLog().getJsonArray("operations").isEmpty());
    });
  }

  @Test
  @DisplayName("Expect an unprocessed event status when an incorrect processing flow is defined")
  void execute_whenEventWithInvalidAddressInKnotFlow_expectEventWithUnprocessedStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    KnotFlow knotFlow = new KnotFlow("invalidAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      Assertions.assertTrue(events.get(0).getLog().getJsonArray("operations").isEmpty());
    });
  }

  @Test
  @DisplayName("Expect an unprocessed event status when the defined Knot does not update the event")
  void execute_whenEventAndNotProcessingKnot_expectEventWithSkippedLogEntryAndUnprocessedStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createNotProcessingKnot(vertx, "aAddress");
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "SKIPPED")
          )));
    });
  }

  @Test
  @DisplayName("Expect a success event status when defined Knot updates the event")
  void execute_whenEventAndProcessingKnot_expectEventWithProcessedLogEntriesAndSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", null);
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED")
          )));
    });
  }

  @Test
  @DisplayName("Expect a success event status when defined Knot updates the event and set next transition")
  void execute_whenEventAndProcessingKnotWithNextTransition_expectEventWithProcessedLogEntriesAndSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", "next");
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED")
          )));
    });
  }

  @Test
  @DisplayName("Expect a failure event status when defined Knot failed")
  void execute_whenEventAndFailingKnot_expectEventWithErrorLogEntryAndFailureStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "bAddress", false);
    KnotFlow knotFlow = new KnotFlow("bAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "ERROR")
          )));
    });
  }

  @Test
  @DisplayName("Expect an exception when defined Knot failed with KnotProcessingFatalException")
  void execute_whenEventAndFailingKnotWithFatalException_expectEngineFailure(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", true);
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    // then
    verifyFailingSingle(testContext, vertx, knotFlow);
  }

  @Test
  @DisplayName("Expect a success event status when two defined Knots update the event")
  void execute_whenEventAndTwoProcessingKnots_expectEventProcessedLogEntriesAndSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", "next");
    createSuccessKnot(vertx, "bAddress", null);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("next", new KnotFlow("bAddress", Collections.emptyMap())));

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "PROCESSED")
          )));
    });
  }

  @Test
  @DisplayName("Expect a success event status when the failing Knots updates the event and the fallback Knot is defined")
  void execute_whenEventAndFailingKnotAndFallbackKnot_expectEventWithErrorAndProcessedLogEntriesAndSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", false);
    createSuccessKnot(vertx, "bAddress", null);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("error", new KnotFlow("bAddress", Collections.emptyMap())));

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "ERROR"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "PROCESSED")
          )));
    });
  }

  @Test
  @DisplayName("Expect a failure event status when the failing Knots updates the event and the fallback Knot does not update the event")
  void execute_whenEventAndFailingKnotAndNotProcessingFallbackKnot_expectFragmentEventWithFailureStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", false);
    createNotProcessingKnot(vertx, "bAddress");
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("error", new KnotFlow("bAddress", Collections.emptyMap())));

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "ERROR"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "SKIPPED")
          )));
    });
  }

  @Test
  @DisplayName("Expect a failure event status when the failing Knots updates the event")
  void execute_whenEventAndFailingKnotAndNoFallbackKnot_expectFragmentEventWithFailureStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", false);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("next", new KnotFlow("someAddress", Collections.emptyMap())));

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      //then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      Assertions.assertTrue(
          verifyLogEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "ERROR")
          )));
    });
  }

  @Test
  @DisplayName("Expect success events statuses when the Knots update events")
  void execute_whenTwoEventsAndProcessingKnot_expectTwoEventWithSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", null);
    createSuccessKnot(vertx, "bAddress", null);

    KnotFlow firstKnotFlow = new KnotFlow("aAddress", Collections.emptyMap());
    KnotFlow secondKnotFlow = new KnotFlow("bAddress", Collections.emptyMap());

    // when
    // when
    verifyExecution(testContext, vertx, Lists.newArrayList(firstKnotFlow, secondKnotFlow),
        events -> {
          //then
          Assertions.assertEquals(2, events.size());
          Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
          Assertions.assertEquals(Status.SUCCESS, events.get(1).getStatus());
        });
  }

  @Test
  @DisplayName("Expect result events in the same order as original")
  void execute_whenTwoEventsAndProcessingKnot_expectTwoEventWithCorrectOrder(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createLongProcessingKnot(vertx, "aAddress", null);
    createSuccessKnot(vertx, "bAddress", null);

    KnotFlow firstKnotFlow = new KnotFlow("aAddress", Collections.emptyMap());
    KnotFlow secondKnotFlow = new KnotFlow("bAddress", Collections.emptyMap());

    // when
    // when
    verifyExecution(testContext, vertx, Lists.newArrayList(firstKnotFlow, secondKnotFlow),
        events -> {
          //then
          Assertions.assertEquals(2, events.size());
          Assertions.assertTrue(
              verifyLogEntries(events.get(0).getLog(), Arrays.asList(
                  Operation.of("aAddress", "RECEIVED"),
                  Operation.of("aAddress", "PROCESSED")
              )));
          Assertions.assertTrue(
              verifyLogEntries(events.get(1).getLog(), Arrays.asList(
                  Operation.of("bAddress", "RECEIVED"),
                  Operation.of("bAddress", "PROCESSED")
              )));
        });
  }

  private void createNotProcessingKnot(Vertx vertx, final String address) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext -> Maybe.empty()
    );
  }

  private void createSuccessKnot(Vertx vertx, String address, String transition) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext ->
        {
          FragmentEvent fragmentEvent = fragmentContext.getFragmentEvent();
          return Maybe.just(new FragmentEventResult(fragmentEvent, transition));
        }
    );
  }

  private void createFailingKnot(Vertx vertx, String address, boolean exitOnError) {
    MockKnotProxy
        .register(vertx.getDelegate(), address,
            new TraceableKnotOptions("next", "error", exitOnError),
            fragmentContext -> {
              Fragment anyFragment = new Fragment("body", new JsonObject(), "");
              throw new KnotProcessingFatalException(anyFragment);
            });
  }

  private void createLongProcessingKnot(Vertx vertx, String address, String transition) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext ->
        {
          FragmentEvent fragmentEvent = fragmentContext.getFragmentEvent();
          SingleSource<FragmentEventResult> emitter =
              singleObserver -> vertx.timerStream(200)
                  .toObservable()
                  .subscribe(
                      time -> singleObserver
                          .onSuccess(new FragmentEventResult(fragmentEvent, transition))
                  );
          return Maybe.fromSingle(emitter);
        }
    );
  }

  private void verifyExecution(VertxTestContext testContext, Vertx vertx, KnotFlow knotFlow,
      Consumer<List<FragmentEvent>> successConsumer) throws Throwable {
    verifyExecution(testContext, vertx, Collections.singletonList(knotFlow), successConsumer);
  }

  private void verifyExecution(VertxTestContext testContext, Vertx vertx, List<KnotFlow> knotFlow,
      Consumer<List<FragmentEvent>> successConsumer) throws Throwable {
    // prepare
    List<FragmentEvent> events = knotFlow.stream()
        .map(flow -> new FragmentEvent(new Fragment("type", new JsonObject(), "body"), flow))
        .collect(
            Collectors.toList());
    KnotEngine engine = KnotEngineFactory.get(vertx, new DeliveryOptions());

    // execute
    Single<List<FragmentEvent>> execute = engine.execute(events, new ClientRequest());

    // verify
    execute.subscribe(
        onSuccess -> testContext.verify(() -> {
          successConsumer.accept(onSuccess);
          testContext.completeNow();
        }), testContext::failNow);

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private void verifyFailingSingle(VertxTestContext testContext, Vertx vertx, KnotFlow knotFlow)
      throws Throwable {
    // given
    KnotEngine engine = KnotEngineFactory.get(vertx, new DeliveryOptions());

    // when
    Single<List<FragmentEvent>> execute = engine
        .execute(Collections
                .singletonList(
                    new FragmentEvent(new Fragment("type", new JsonObject(), "body"), knotFlow)),
            new ClientRequest());

    // then
    execute.subscribe(
        onSuccess -> testContext.failNow(new IllegalStateException()),
        error -> testContext.completeNow());

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

}