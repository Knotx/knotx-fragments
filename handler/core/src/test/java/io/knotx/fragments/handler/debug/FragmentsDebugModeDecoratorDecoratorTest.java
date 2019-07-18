package io.knotx.fragments.handler.debug;

import static io.knotx.fragments.api.Fragment.JSON_OBJECT_TYPE;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.knotx.fragments.engine.Task;
import io.vertx.core.json.JsonObject;

class FragmentsDebugModeDecoratorDecoratorTest {

  @Test
  @DisplayName("Expect fragment event marked as debuggable and original body in the debug data")
  void expectFragmentEventMarkedAsDebuggable() {
    // given
    FragmentsDebugModeDecorator tested = FragmentsDebugModeDecorator.SNIPPET_TYPE;

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
    tested.markAsDebuggable(true, singletonList(fragmentEventContextTaskAware));

    // then
    assertTrue(debugData.getBoolean("debug"));
    assertEquals(expectedBody, debugData.getString("body"));
  }

  @Test
  @DisplayName("Expect fragment event not marked as debuggable when no task is defined")
  void expectFragmentEventNotMarkedAsDebuggable() {
    // given
    FragmentsDebugModeDecorator tested = FragmentsDebugModeDecorator.SNIPPET_TYPE;

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
    tested.markAsDebuggable(true, singletonList(fragmentEventContextTaskAware));

    // then
    assertFalse(debugData.containsKey("debug"));
    assertTrue(debugData.isEmpty());
  }

  @Test
  @DisplayName("Expect fragment debug data calculated when Fragment is debuggable snippet")
  void expectDebugDataCalculatedWhenFragmentMarkedAsDebuggable() {
    // given
    FragmentsDebugModeDecorator tested = FragmentsDebugModeDecorator.SNIPPET_TYPE;

    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    String body = "test";
    JsonObject expectedPayload = new JsonObject().put("test", "value");

    Fragment fragment = new Fragment(Fragment.SNIPPET_TYPE, new JsonObject(), body);
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
    tested.addDebugAssetsAndData(true, singletonList(fragmentEvent));

    // then
    assertEquals(expectedPayload, debugData.getJsonObject("payload"));
    assertEquals(expectedJsonLog, debugData.getJsonObject("logs"));
    assertEquals(expectedBody, fragment.getBody());
  }

  @Test
  @DisplayName("Expect fragment with body end tag contains debug data and scripts for snippet")
  void expectFragmentWithBodyEndTagContainsDebugDataAndScripts() throws IOException {
    //given
    FragmentsDebugModeDecorator tested = FragmentsDebugModeDecorator.SNIPPET_TYPE;

    String originalBody = "</body>";
    Fragment fragmentWithBodyEndTag = new Fragment(Fragment.SNIPPET_TYPE, new JsonObject(), originalBody);
    FragmentEvent fragmentEventWithBodyEndTag = new FragmentEvent(fragmentWithBodyEndTag);
    InputStream debugCssIs = getClass().getClassLoader()
        .getResourceAsStream("debug/debug.css");
    InputStream debugJsIs = getClass().getClassLoader()
        .getResourceAsStream("debug/debug.js");
    String debugCss = IOUtils.toString(debugCssIs, StandardCharsets.UTF_8);
    String debugJs = IOUtils.toString(debugJsIs, StandardCharsets.UTF_8);

    //when
    tested.addDebugAssetsAndData(true, singletonList(fragmentEventWithBodyEndTag));

    //then
    String expectedBody = fragmentEventWithBodyEndTag.getFragment()
        .getBody();
    assertTrue(expectedBody.contains("var debugData"));
    assertTrue(expectedBody.contains(debugCss));
    assertTrue(expectedBody.contains(debugJs));
  }

  @Test
  @DisplayName("Expect fragment with debug data for Json Object")
  void expectFragmentWithDebugDataForJson(){
    //given
    FragmentsDebugModeDecorator tested = FragmentsDebugModeDecorator.JSON_OBJECT_TYPE;

    String originalBody = "{\"debug\":{}}";
    Fragment jsonObjectFragment = new Fragment(JSON_OBJECT_TYPE, new JsonObject(), originalBody);
    FragmentEvent jsonObjectFragmentEvent = new FragmentEvent(jsonObjectFragment);
    jsonObjectFragmentEvent.getDebugData().put("debug", "someValue");

    //when
    tested.addDebugAssetsAndData(true, singletonList(jsonObjectFragmentEvent));

    //then
    JsonObject expectedBody = new JsonObject(jsonObjectFragmentEvent.getFragment().getBody());

    assertFalse(expectedBody.getJsonObject("debug").getMap().values().isEmpty());
  }
}