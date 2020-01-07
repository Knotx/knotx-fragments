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
package io.knotx.fragments.handler.consumer;

import static com.google.common.base.Predicates.alwaysTrue;
import static io.knotx.fragments.handler.consumer.DebugDataRetriever.retrieveDebugData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;

public class DebugDataRetrieverTest {

  @Test
  @DisplayName("Expect calculated debug data as FragmentEvent logs, fragment payload and body with fragment id as key")
  void expectCalculatedDebugDataAsFragmentEventLog() {
    // given
    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    JsonObject logData = new JsonObject();
    JsonObject payload = new JsonObject();
    String body = "body";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    fragment.mergeInPayload(payload);

    when(fragmentEvent.getFragment()).thenReturn(fragment);
    when(fragmentEvent.getLogAsJson()).thenReturn(logData);

    // when
    JsonObject debugData = retrieveDebugData(ImmutableList.of(fragmentEvent), alwaysTrue());

    // then
    assertEquals(1, debugData.size());
    assertEquals(logData, debugData.getJsonObject(fragment.getId()).getJsonObject("logs"));
    assertEquals(payload, debugData.getJsonObject(fragment.getId()).getJsonObject("payload"));
    assertEquals(body, debugData.getJsonObject(fragment.getId()).getString("body"));
  }
}
