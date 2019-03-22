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
package io.knotx.engine.core;

import static io.knotx.engine.core.FlowEntryLogVerifier.verifyLogEntries;
import static io.knotx.engine.core.FlowExecutionVerifier.verifyExecution;
import static io.knotx.engine.core.KnotFactory.createLongProcessingKnot;
import static io.knotx.engine.core.KnotFactory.createSuccessKnot;

import com.google.common.collect.Lists;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.core.FlowEntryLogVerifier.Operation;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class KnotEngineProcessingOrderTest {

  @Test
  @DisplayName("Expect events in the same order as original")
  void execute_whenTwoEventsAndProcessingKnot_expectTwoEventWithCorrectOrder(
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    createLongProcessingKnot(vertx, "aAddress", null, 200);
    createSuccessKnot(vertx, "bAddress", null);

    KnotFlow firstKnotFlow = new KnotFlow("aAddress", Collections.emptyMap());
    KnotFlow secondKnotFlow = new KnotFlow("bAddress", Collections.emptyMap());

    // when
    // when
    verifyExecution(testContext, vertx, Lists.newArrayList(firstKnotFlow, secondKnotFlow),
        events -> {
          //then
          Assertions.assertEquals(2, events.size());
          verifyLogEntries(events.get(0).getLog(),
              Operation.of("aAddress", "RECEIVED"),
              Operation.of("aAddress", "PROCESSED")
          );
          verifyLogEntries(events.get(1).getLog(),
              Operation.of("bAddress", "RECEIVED"),
              Operation.of("bAddress", "PROCESSED")
          );
        });
  }

}
