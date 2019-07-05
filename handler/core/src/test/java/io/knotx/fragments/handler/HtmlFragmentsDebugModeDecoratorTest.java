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
package io.knotx.fragments.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.knotx.fragments.engine.Task;
import io.vertx.core.json.JsonObject;

class HtmlFragmentsDebugModeDecoratorTest {

  private HtmlFragmentsDebugModeDecorator tested;

  @BeforeEach
  void setUp() {
    tested = new HtmlFragmentsDebugModeDecorator();
    tested.init();
  }

  @Test
  @DisplayName("Expect fragment event marked as debuggable and original body in the debug data")
  void expectFragmentEventMarkedAsDebuggable() {
    // given
    FragmentEventContextTaskAware fragmentEventContextTaskAware = Mockito
        .mock(FragmentEventContextTaskAware.class);
    FragmentEventContext fragmentEventContext = Mockito.mock(FragmentEventContext.class);
    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    Fragment fragment = Mockito.mock(Fragment.class);
    JsonObject debugData = new JsonObject();
    String expectedBody = "expected body";
    when(fragmentEventContextTaskAware.getTask()).thenReturn(Optional.of(new Task("name")));
    when(fragmentEventContextTaskAware.getFragmentEventContext()).thenReturn(fragmentEventContext);
    when(fragmentEventContext.getFragmentEvent()).thenReturn(fragmentEvent);
    when(fragmentEvent.getFragment()).thenReturn(fragment);
    when(fragmentEvent.getDebugData()).thenReturn(debugData);
    when(fragment.getBody()).thenReturn(expectedBody);

    // when
    tested.markAsDebuggable(fragmentEventContextTaskAware);

    // then
    assertTrue(debugData.getBoolean("debug"));
    assertEquals(expectedBody, debugData.getString("body"));
  }

  @Test
  @DisplayName("Expect fragment event not marked as debuggable when no task is defined")
  void expectFragmentEventNotMarkedAsDebuggable() {
    // given
    FragmentEventContextTaskAware fragmentEventContextTaskAware = Mockito
        .mock(FragmentEventContextTaskAware.class);
    FragmentEventContext fragmentEventContext = Mockito.mock(FragmentEventContext.class);
    Fragment fragment = new Fragment("test", new JsonObject(), "");
    FragmentEvent fragmentEvent = new FragmentEvent(fragment);
    JsonObject debugData = new JsonObject();
    when(fragmentEventContextTaskAware.getTask()).thenReturn(Optional.empty());
    when(fragmentEventContextTaskAware.getFragmentEventContext()).thenReturn(fragmentEventContext);
    when(fragmentEventContext.getFragmentEvent()).thenReturn(fragmentEvent);

    // when
    tested.markAsDebuggable(fragmentEventContextTaskAware);

    // then
    assertFalse(debugData.containsKey("debug"));
    assertTrue(debugData.isEmpty());
  }

  @Test
  @DisplayName("Expect fragment debug data calculated when Fragment is debuggable")
  void expectDebugDataCalculatedWhenFragmentMarkedAsDebuggable() {
    // given
    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    String body = "test";
    JsonObject expectedPayload = new JsonObject().put("test", "value");

    Fragment fragment = new Fragment("test", new JsonObject(), body);
    fragment.mergeInPayload(expectedPayload);
    String expectedBody = "<!-- data-knotx-id='" + fragment.getId() + "' -->"
        + body
        + "<!-- data-knotx-id='" + fragment.getId() + "' -->";
    JsonObject debugData = new JsonObject().put("debug", true);
    when(fragmentEvent.getFragment()).thenReturn(fragment);
    when(fragmentEvent.getDebugData()).thenReturn(debugData);
    JsonObject expectedJsonLog = new JsonObject().put("log", "entry");
    when(fragmentEvent.getLogAsJson()).thenReturn(expectedJsonLog);
    // when
    tested.addDebugAssetsAndData(Collections.singletonList(fragmentEvent));

    // then
    assertEquals(expectedPayload, debugData.getJsonObject("payload"));
    assertEquals(expectedJsonLog, debugData.getJsonObject("logs"));
    assertEquals(expectedBody, fragment.getBody());
  }

  @Test
  @DisplayName("Expect fragment with body end tag contains debug data and scripts")
  void expectFragmentWithBodyEndTagContainsDebugDataAndScripts() throws IOException {
    //given
    String originalBody = "</body>";
    Fragment fragmentWithBodyEndTag = new Fragment("test", new JsonObject(), originalBody);
    FragmentEvent fragmentEventWithBodyEndTag = new FragmentEvent(fragmentWithBodyEndTag);
    InputStream debugCssIs = getClass().getClassLoader()
        .getResourceAsStream("debug/debug.css");
    InputStream debugJsIs = getClass().getClassLoader()
        .getResourceAsStream("debug/debug.js");
    String debugCss = IOUtils.toString(debugCssIs, StandardCharsets.UTF_8);
    String debugJs = IOUtils.toString(debugJsIs, StandardCharsets.UTF_8);

    //when
    tested.addDebugAssetsAndData(Collections.singletonList(fragmentEventWithBodyEndTag));

    //then
    String expectedBody = fragmentEventWithBodyEndTag.getFragment()
        .getBody();
    assertTrue(expectedBody.contains("var debugData"));
    assertTrue(expectedBody.contains(debugCss));
    assertTrue(expectedBody.contains(debugJs));
  }
}