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
package io.knotx.fragments.handler.consumer.json;

import static io.knotx.fragments.engine.api.EventLogEntry.NodeStatus.UNPROCESSED;
import static io.knotx.fragments.handler.consumer.json.JsonFragmentsHandlerConsumerFactory.CONDITION_OPTION;
import static io.knotx.fragments.handler.consumer.json.JsonFragmentsHandlerConsumerFactory.FRAGMENT_TYPES_OPTIONS;
import static io.knotx.fragments.handler.consumer.json.JsonFragmentsHandlerConsumerFactory.HEADER_OPTION;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.api.FragmentEvent;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonFragmentsHandlerConsumerFactoryTest {

  private static final String EXPECTED_FRAGMENT_TYPE = "snippet";
  private static final String EXPECTED_HEADER = "x-knotx-debug";
  private static final String EXPECTED_PARAM = "debug";
  private static final String PARAM_OPTION = "param";
  private static final String HTML_TYPE = "html";
  private static final String FRAGMENT_BODY_JSON = "{\"user\": \"admin\"}";
  private static final String FRAGMENT_BODY_HTML = "\"<div>body</div>\"";
  private static final String USER_KEY = "user";
  private static final String KNOTX_FRAGMENT_KEY = "_knotx_fragment";

  private static Stream<Arguments> provideFragmentConsumerConfiguration() {
    return Stream.of(
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_JSON),
        Arguments.of(HTML_TYPE, FRAGMENT_BODY_HTML),
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_HTML),
        Arguments.of(HTML_TYPE, FRAGMENT_BODY_JSON)
    );
  }

  @ParameterizedTest
  @MethodSource("provideFragmentConsumerConfiguration")
  @DisplayName("Fragment body should not be modified when invalid configuration provided")
  void fragmentBodyShouldNotBeModifiedWhenInvalidConfigurationProvided(String fragmentType,
      String fragmentBody) {
    FragmentEvent original = new FragmentEvent(
        new Fragment(fragmentType, new JsonObject(), fragmentBody));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Fragment should not be modified when supported fragments do not contain fragment type")
  void expectFragmentNotModifiedWhenSupportedFragmentsDoNotContainFragmentType() {
    //given
    FragmentEvent original = new FragmentEvent(
        new Fragment(HTML_TYPE, new JsonObject(), FRAGMENT_BODY_JSON));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    //when
    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));
    //then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Fragment should be modified when header condition and fragment type match")
  void expectFragmentModifiedWhenHederConditionAndSupportedTypedConfigured() {
    //given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    //when
    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    //then
    assertNotEquals(copy, original);
  }

  @Test
  @DisplayName("Fragment should be modified when param condition and fragment type match")
  void expectFragmentModifiedWhenParamConditionAndSupportedTypesConfigured() {
    //given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    //when
    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    //then
    assertNotEquals(copy, original);
  }

  @Test
  @DisplayName("Execution log entry should be properly merged into existing fragment body")
  void expectExecutionLogEntryProperlyMergedIntoFragmentBody() {
    //given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    //when
    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    //then
    assertNotEquals(copy, original);
    JsonObject fragmentBody = new JsonObject(original.getFragment().getBody());
    assertTrue(fragmentBody.containsKey(USER_KEY));
    assertTrue(fragmentBody.containsKey(KNOTX_FRAGMENT_KEY));
  }

  @Test
  @DisplayName("Execution log entry should contain proper fragment details")
  void expectFragmentDetailsInExecutionLogEntry() {
    //given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    JsonObject expectedLog = new JsonObject()
        .put("startTime", 0)
        .put("finishTime", 0)
        .put("status", UNPROCESSED)
        .put("fragment", new JsonObject()
            .put("id", original.getFragment().getId())
            .put("type", "snippet")
            .put("body", FRAGMENT_BODY_JSON))
        .put("graph", new JsonObject());

    //when
    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    //then
    assertNotEquals(copy, original);
    JsonObject fragmentBody = new JsonObject(original.getFragment().getBody());
    assertJsonEquals(expectedLog, fragmentBody.getJsonObject(KNOTX_FRAGMENT_KEY));
    assertEquals(new JsonObject(FRAGMENT_BODY_JSON).getString(USER_KEY),
        fragmentBody.getString(USER_KEY));
  }
}