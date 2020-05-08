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
package io.knotx.fragments.task.handler.log.json;

import static io.knotx.fragments.task.handler.log.json.JsonFragmentsHandlerConsumerFactory.CONDITION_OPTION;
import static io.knotx.fragments.task.handler.log.json.JsonFragmentsHandlerConsumerFactory.FRAGMENT_TYPES_OPTIONS;
import static io.knotx.fragments.task.handler.log.json.JsonFragmentsHandlerConsumerFactory.HEADER_OPTION;
import static io.knotx.fragments.task.handler.log.json.JsonFragmentsHandlerConsumerFactory.KNOTX_FRAGMENT;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.handler.log.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.task.handler.log.api.model.FragmentExecutionLog;
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

  private static final String EXPECTED_FRAGMENT_TYPE = "json";
  private static final String EXPECTED_HEADER = "x-knotx-debug";
  private static final String EXPECTED_PARAM = "debug";
  private static final String PARAM_OPTION = "param";
  private static final String OTHER_TYPE = "other";
  private static final String FRAGMENT_BODY_JSON = "{\"user\": \"admin\"}";
  private static final String FRAGMENT_BODY_OTHER = "\"simple text value\"";
  private static final String USER_KEY = "user";
  private static final String UNSUPPORTED = "unsupported";

  private static Stream<Arguments> unfulfilledConditions() {
    return Stream.of( //fragmentType, fragmentBody, supportedTypes
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_JSON, new JsonArray()),
        Arguments.of(OTHER_TYPE, FRAGMENT_BODY_OTHER, new JsonArray()),
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_OTHER, new JsonArray()),
        Arguments.of(OTHER_TYPE, FRAGMENT_BODY_JSON, new JsonArray()),
        Arguments.of(OTHER_TYPE, FRAGMENT_BODY_OTHER, new JsonArray().add(EXPECTED_FRAGMENT_TYPE)),
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_OTHER,
            new JsonArray().add(EXPECTED_FRAGMENT_TYPE)),
        Arguments.of(OTHER_TYPE, FRAGMENT_BODY_JSON, new JsonArray().add(EXPECTED_FRAGMENT_TYPE)),
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_JSON, new JsonArray().add(UNSUPPORTED)),
        Arguments.of(OTHER_TYPE, FRAGMENT_BODY_OTHER, new JsonArray().add(UNSUPPORTED)),
        Arguments.of(EXPECTED_FRAGMENT_TYPE, FRAGMENT_BODY_OTHER, new JsonArray().add(UNSUPPORTED)),
        Arguments.of(OTHER_TYPE, FRAGMENT_BODY_JSON, new JsonArray().add(UNSUPPORTED))
    );
  }

  @ParameterizedTest
  @MethodSource("unfulfilledConditions")
  @DisplayName("Fragment body should not be modified when no supported methods specified in configuration")
  void fragmentBodyShouldNotBeModifiedWhenInvalidConfigurationProvided(String fragmentType,
      String fragmentBody, JsonArray supportedTypes) {
    Fragment original = new Fragment(fragmentType, new JsonObject(), fragmentBody);
    Fragment copy = new Fragment(original.toJson());

    FragmentExecutionLogConsumer tested = new JsonFragmentsHandlerConsumerFactory()
        .create(new JsonObject()
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER))
            .put(FRAGMENT_TYPES_OPTIONS, supportedTypes));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Fragment should be modified when header condition and fragment type match")
  void expectFragmentModifiedWhenHeaderConditionAndSupportedTypedConfigured() {
    //given
    Fragment original = new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON);
    Fragment copy = new Fragment(original.toJson());

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
    Fragment original = new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON);
    Fragment copy = new Fragment(original.toJson());

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
    Fragment original = new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON);
    Fragment copy = new Fragment(original.toJson());

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
    JsonObject fragmentBody = new JsonObject(original.getBody());
    assertTrue(fragmentBody.containsKey(USER_KEY));
    assertTrue(fragmentBody.containsKey(KNOTX_FRAGMENT));
  }

  @Test
  @DisplayName("Execution log entry should contain proper fragment details")
  void expectFragmentDetailsInExecutionLogEntry() {
    //given
    Fragment original = new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(), FRAGMENT_BODY_JSON);
    Fragment copy = new Fragment(original.toJson());

    JsonObject expectedLog = FragmentExecutionLog.newInstance(original).toJson();

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
    JsonObject fragmentBody = new JsonObject(original.getBody());
    assertJsonEquals(expectedLog,
        new FragmentExecutionLog(fragmentBody.getJsonObject(KNOTX_FRAGMENT)).toJson());
    assertEquals(new JsonObject(FRAGMENT_BODY_JSON).getString(USER_KEY),
        fragmentBody.getString(USER_KEY));
  }
}