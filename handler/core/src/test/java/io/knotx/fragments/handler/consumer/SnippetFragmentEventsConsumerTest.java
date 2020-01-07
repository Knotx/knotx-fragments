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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;

class SnippetFragmentEventsConsumerTest {

  @Test
  @DisplayName("Expect snippet fragment body is wrapped by fragmentId")
  void expectSnippetFragmentBodyWrappedByFragmentId() {
    // given
    SnippetFragmentEventsConsumer tested = new SnippetFragmentEventsConsumer("debugCss", "debugJs");

    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    String body = "body";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);

    String expectedBody = "<!-- data-knotx-id='" + fragment.getId() + "' -->"
        + body
        + "<!-- data-knotx-id='" + fragment.getId() + "' -->";

    when(fragmentEvent.getFragment()).thenReturn(fragment);

    // when
    tested.wrapWithFragmentId(ImmutableList.of(fragmentEvent));

    // then
    assertEquals(expectedBody, fragment.getBody());
  }

  @Test
  @DisplayName("Expect css and js appended with debug data")
  void expectCssJsAppendedWithDebugData() throws IOException {
    // given
    String debugCss = "debugCss";
    String debugJs = "debugJs";

    SnippetFragmentEventsConsumer tested = new SnippetFragmentEventsConsumer(debugCss, debugJs);

    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    JsonObject logData = new JsonObject();
    Fragment fragment = new Fragment("snippet", new JsonObject(), "</body>");

    when(fragmentEvent.getFragment()).thenReturn(fragment);
    when(fragmentEvent.getLogAsJson()).thenReturn(logData);

    // when
    tested.appendWithDebugData(ImmutableList.of(fragmentEvent), logData);

    // then
    assertTrue(fragment.getBody().contains(debugCss));
    assertTrue(fragment.getBody().contains(debugJs));
    assertTrue(fragment.getBody().contains("var debugData = { }"));
  }
}