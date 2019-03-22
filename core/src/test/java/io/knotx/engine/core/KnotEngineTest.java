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

import static io.knotx.engine.core.FlowEntryLogVerifier.verifyLogEntries;
import static io.knotx.engine.core.FlowExecutionVerifier.verifyExecution;
import static io.knotx.engine.core.FlowExecutionVerifier.verifyFailingSingle;
import static io.knotx.engine.core.KnotFactory.createFailingKnot;
import static io.knotx.engine.core.KnotFactory.createLongProcessingKnot;
import static io.knotx.engine.core.KnotFactory.createNotProcessingKnot;
import static io.knotx.engine.core.KnotFactory.createSuccessKnot;

import com.google.common.collect.Lists;
import io.knotx.engine.api.FragmentEvent.Status;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.api.KnotFlowStep;
import io.knotx.engine.core.FlowEntryLogVerifier.Operation;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class KnotEngineTest {

  @Test
  @DisplayName("Expect unprocessed status when no flow is defined")
  void executeEventWithNoKnot(VertxTestContext testContext, Vertx vertx) throws Throwable {
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
  @DisplayName("Expect failure status when invalid flow is defined")
  void executeEventWithInvalidAddressInKnotFlow(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    KnotFlow knotFlow = new KnotFlow("invalidAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("invalidAddress", "ERROR")
      );
    });
  }

  @Test
  @DisplayName("Expect unprocessed status when Knot does not process event")
  void executeEventWithNotProcessingKnot(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    createNotProcessingKnot(vertx, "aAddress");
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.UNPROCESSED, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "SKIPPED")
      );
    });
  }

  @Test
  @DisplayName("Expect success status when Knot processes event")
  void executeEventWithProcessingKnot(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", null);
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "PROCESSED")
      );
    });
  }

  @Test
  @DisplayName("Expect success status when Knot processes event and set 'next' transition")
  void executeEventWithProcessingKnotWithNextTransition(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    createSuccessKnot(vertx, "aAddress", "next");
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.SUCCESS, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "PROCESSED")
      );
    });
  }

  @Test
  @DisplayName("Expect failure status when Knot fails")
  void executeEventWithFailingKnot(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createFailingKnot(vertx, "bAddress", false);
    KnotFlow knotFlow = new KnotFlow("bAddress", Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("bAddress", "RECEIVED"),
          Operation.of("bAddress", "ERROR")
      );
    });
  }

  // TODO introduce timeout log entry
  @Test
  @DisplayName("Expect failure status when Knot times out")
  void executeEventWithLongRunningKnot(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    int processingTime = 1000;
    int processingTimeout = 500;

    createLongProcessingKnot(vertx, "bAddress", "next", processingTime);
    KnotFlowStep flowStep = new KnotFlowStep("bAddress",
        new DeliveryOptions().setSendTimeout(processingTimeout));
    KnotFlow knotFlow = new KnotFlow(flowStep, Collections.emptyMap());

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      // then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("bAddress", "TIMEOUT")
      );
    });
  }

  @Test
  @DisplayName("Expect exception when Knot failed with KnotProcessingFatalException")
  void executeEventWithFailingKnotWithFatalException(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", true);
    KnotFlow knotFlow = new KnotFlow("aAddress", Collections.emptyMap());

    // when
    // then
    verifyFailingSingle(testContext, vertx, knotFlow);
  }

  @Test
  @DisplayName("Expect success status when two Knots update the event")
  void executeEventWithTwoProcessingKnots(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
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
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "PROCESSED"),
          Operation.of("bAddress", "RECEIVED"),
          Operation.of("bAddress", "PROCESSED")
      );
    });
  }

  @Test
  @DisplayName("Expect success status when first Knot fails and the fallback Knot processes")
  void executeEventWithFailingKnotAndFallbackKnot(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
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
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "ERROR"),
          Operation.of("bAddress", "RECEIVED"),
          Operation.of("bAddress", "PROCESSED")
      );
    });
  }

  @Test
  @DisplayName("Expect failure status when first Knot fails and fallback Knot does not process")
  void executeEventWithFailingKnotAndNotProcessingFallbackKnot(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
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
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "ERROR"),
          Operation.of("bAddress", "RECEIVED"),
          Operation.of("bAddress", "SKIPPED")
      );
    });
  }

  @Test
  @DisplayName("Expect failure status and flow not continued when first Knot in flow fails")
  void executeEventAndFailingKnotAndNoFallbackKnot(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    createFailingKnot(vertx, "aAddress", false);
    KnotFlow knotFlow = new KnotFlow("aAddress",
        Collections.singletonMap("next", new KnotFlow("someAddress", Collections.emptyMap())));

    // when
    verifyExecution(testContext, vertx, knotFlow, events -> {
      //then
      Assertions.assertEquals(1, events.size());
      Assertions.assertEquals(Status.FAILURE, events.get(0).getStatus());
      verifyLogEntries(events.get(0).getLog(),
          Operation.of("aAddress", "RECEIVED"),
          Operation.of("aAddress", "ERROR")
      );
    });
  }

  @Test
  @DisplayName("Expect two success statuses when Knots processes")
  void executeTwoEventsAndProcessingKnot(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
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
}