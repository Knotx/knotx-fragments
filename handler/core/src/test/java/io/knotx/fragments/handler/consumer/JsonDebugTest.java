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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;

class JsonDebugTest {
  @Test
  @DisplayName("Expect Json fragment body with debug data")
  void expectJsonFragmentBodyWithDebugData() {
    // given
    JsonDebug tested = new JsonDebug();

    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    String body = "{}";
    Fragment fragment = new Fragment("json", new JsonObject(), body);

    when(fragmentEvent.getFragment()).thenReturn(fragment);
    when(fragmentEvent.getLogAsJson()).thenReturn(new JsonObject());

    // when
    tested.accept(ImmutableList.of(fragmentEvent));

    // then
    JsonObject debugData = new JsonObject(fragment.getBody())
        .getJsonObject("debug")
        .getJsonObject(fragment.getId());
    assertTrue(Objects.nonNull(debugData));
    assertTrue(debugData.containsKey("payload"));
    assertTrue(debugData.containsKey("logs"));
    assertTrue(debugData.containsKey("body"));
  }
  @Test
  @DisplayName("Expect body not changed for non json fragment type")
  void expectBodyNotChangedForNonJsonFragment() {
    // given
    JsonDebug tested = new JsonDebug();

    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    String body = "{}";
    Fragment fragment = new Fragment("custom", new JsonObject(), body);

    when(fragmentEvent.getFragment()).thenReturn(fragment);

    // when
    tested.accept(ImmutableList.of(fragmentEvent));

    // then
    assertEquals(body, fragment.getBody());
  }
}