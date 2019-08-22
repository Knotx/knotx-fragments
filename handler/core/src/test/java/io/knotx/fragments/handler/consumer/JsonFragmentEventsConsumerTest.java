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

class JsonFragmentEventsConsumerTest {
  @Test
  @DisplayName("Expect Json fragment body with debug data")
  void expectJsonFragmentBodyWithDebugData() {
    // given
    JsonFragmentEventsConsumer tested = new JsonFragmentEventsConsumer();

    FragmentEvent fragmentEvent = Mockito.mock(FragmentEvent.class);
    String body = "{}";
    Fragment fragment = new Fragment("json", new JsonObject(), body);

    when(fragmentEvent.getFragment()).thenReturn(fragment);
    when(fragmentEvent.getLogAsJson()).thenReturn(new JsonObject());

    // when
    tested.accept(ImmutableList.of(fragmentEvent));

    // then
    JsonObject debugData = new JsonObject(fragment.getBody()).getJsonObject("debug");
    assertTrue(Objects.nonNull(debugData));
    assertTrue(debugData.containsKey("payload"));
    assertTrue(debugData.containsKey("logs"));
    assertTrue(debugData.containsKey("body"));
  }
  @Test
  @DisplayName("Expect body not changed for non json fragment type")
  void expectBodyNotChangedForNonJsonFragment() {
    // given
    JsonFragmentEventsConsumer tested = new JsonFragmentEventsConsumer();

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