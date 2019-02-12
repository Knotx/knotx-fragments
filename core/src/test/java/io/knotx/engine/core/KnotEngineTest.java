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

import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEvent.Status;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.api.KnotProcessingFatalException;
import io.knotx.engine.api.TraceableKnotOptions;
import io.knotx.engine.core.EntryLogTestHelper.Operation;
import io.knotx.fragment.Fragment;
import io.knotx.knotengine.core.junit.MockKnotProxy;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Maybe;
import io.reactivex.Single;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class KnotEngineTest {

  @Test
  public void execute_whenFragmentWithNoKnot_expectFragmentEventWithUnprocessedStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    KnotFlow knotFlow = null;

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      Assertions.assertTrue(events.get(0).getLog().getJsonArray("operations").isEmpty());
    });
  }

  @Test
  public void execute_whenFragmentWithInvalidKnotAddress_expectFragmentEventWithUnprocessedStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    KnotFlow knotFlow = new KnotFlow("invalidAddress", Collections.emptyMap());

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      Assertions.assertTrue(events.get(0).getLog().getJsonArray("operations").isEmpty());
    });
  }

  @Test
  public void execute_whenFragmentThatIsNotProcessedByKnot_expectFragmentEventWithVerifyingLogEntryAndUnprocessedStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createNotProcessingKnot(vertx, "aAddress");
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "SKIPPED")
          )));
    });
  }

  @Test
  public void execute_whenFragmentWithOneKnot_expectFragmentEventWithReceivedAndProcessedLogEntriesAndSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", "next");
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED")
          )));
    });
  }

  @Test
  public void execute_whenFragmentWithFailingKnot_expectFragmentEventWithReceivedAndErrorLogEntriesAndFailureStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", false);
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "ERROR")
          )));
    });
  }

  @Test
  public void execute_whenFragmentWithHardFailingKnot_expectFailure(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", true);
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // then
    verifyFailingSingle(testContext, vertx, knotFlow);
  }

  @Test
  public void execute_whenFragmentWithTwoKnots_expectFragmentEventReceivedProcessedLogEntriesAndSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // when
    createSuccessKnot(vertx, "aAddress", "next");
    createSuccessKnot(vertx, "bAddress", null);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("next", new KnotFlow("bAddress", Collections.emptyMap())));

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "PROCESSED")
          )));
    });
  }

  @Test
  public void execute_whenFragmentWithFailingKnotAndFallback_expectFragmentEventWithSuccessStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // when
    createFailingKnot(vertx, "aAddress", false);
    createSuccessKnot(vertx, "bAddress", null);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("error", new KnotFlow("bAddress", Collections.emptyMap())));

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "ERROR"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "PROCESSED")
          )));
    });
  }

  @Test
  public void execute_whenFragmentWithFailingKnotAndNotProcessingFallback_expectFragmentEventWithFailureStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // when
    createFailingKnot(vertx, "aAddress", false);
    createNotProcessingKnot(vertx, "bAddress");
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("error", new KnotFlow("bAddress", Collections.emptyMap())));

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "ERROR"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "SKIPPED")
          )));
    });
  }

  @Test
  public void execute_whenFragmentWithFailingKnotAndNoFallback_expectFragmentEventWithFailureStatus(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // when
    createSuccessKnot(vertx, "aAddress", "next");
    createFailingKnot(vertx, "bAddress", false);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("next", new KnotFlow("bAddress", Collections.emptyMap())));

    // then
    verifySingle(testContext, vertx, knotFlow, events -> {
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      Assertions.assertTrue(
          EntryLogTestHelper.containsEntries(events.get(0).getLog(), Arrays.asList(
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED"),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "ERROR")
          )));
    });
  }

  private void createNotProcessingKnot(Vertx vertx, final String address) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext -> Maybe.empty()
    );
  }

  private void createSuccessKnot(Vertx vertx, final String address, final String transition) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext ->
        {
          FragmentEvent fragmentEvent = fragmentContext.getFragmentEvent();
          return Maybe.just(new FragmentEventResult(fragmentEvent, transition));
        }
    );
  }

  private void createFailingKnot(Vertx vertx, final String address, boolean exitOnError) {
    MockKnotProxy
        .register(vertx.getDelegate(), address,
            new TraceableKnotOptions("next", "error", exitOnError),
            fragmentContext -> {
              Fragment anyFragment = new Fragment("body", new JsonObject(), "");
              throw new KnotProcessingFatalException(anyFragment);
            });
  }

  private void verifySingle(VertxTestContext testContext, Vertx vertx, KnotFlow knotFlow,
      Consumer<List<FragmentEvent>> successConsumer) throws Throwable {
    // given
    KnotEngine engine = new KnotEngine(vertx,
        new KnotEngineHandlerOptions(Collections.emptyList(), new DeliveryOptions()));

    // when
    Single<List<FragmentEvent>> execute = engine
        .execute(Collections
                .singletonList(
                    new FragmentEvent(new Fragment("type", new JsonObject(), "body"), knotFlow)),
            new ClientRequest());

    // then
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
    KnotEngine engine = new KnotEngine(vertx,
        new KnotEngineHandlerOptions(Collections.emptyList(), new DeliveryOptions()));

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