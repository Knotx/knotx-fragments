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
package io.knotx.fragments.handler.consumer.html;

import static io.knotx.fragments.handler.consumer.html.FragmentHtmlBodyWriterFactory.CONDITION_OPTION;
import static io.knotx.fragments.handler.consumer.html.FragmentHtmlBodyWriterFactory.FRAGMENT_TYPES_OPTIONS;
import static io.knotx.fragments.handler.consumer.html.FragmentHtmlBodyWriterFactory.HEADER_OPTION;
import static io.knotx.junit5.assertions.KnotxAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.api.EventLogEntry.NodeStatus;
import io.knotx.fragments.engine.api.FragmentEvent;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.fragments.handler.consumer.api.model.GraphNodeExecutionLog;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentHtmlBodyWriterFactoryTest {

  private static final String EXPECTED_FRAGMENT_TYPE = "snippet";
  private static final String EXPECTED_HEADER = "x-knotx-debug";
  private static final String EXPECTED_PARAM = "debug";
  private static final String PARAM_OPTION = "param";

  @Test
  @DisplayName("Expect fragment is not modified when condition not configured")
  void expectFragmentNotModifiedWhenConditionNotConfigured() {
    // given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE)));
    tested
        .accept(new ClientRequest(), ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    // then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is not modified when supported fragments types not configured.")
  void expectFragmentNotModifiedWhenSupportedTypesNotConfigured() {
    // given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    // then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is not modified when supported fragments does not contain fragment type.")
  void expectFragmentNotModifiedWhenOtherSupportedTypeConfigured() {
    // given
    FragmentEvent original = new FragmentEvent(new Fragment("json", new JsonObject(),
        "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    // then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is modified when header condition and supported type configured.")
  void expectFragmentBodyModifiedWhenHeaderConditionConfigured() {
    // given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    // then
    assertNotEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is modified when param condition and supported type configured.")
  void expectFragmentBodyModifiedWhenParamConditionConfigured() {
    // given
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(original)));

    // then
    assertNotEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment body is wrapped by fragmentId.")
  void expectFragmentBodyWrappedByFragmentId() {
    // given
    String body = "<div>body</div>";
    FragmentEvent event = new FragmentEvent(new Fragment("snippet", new JsonObject(), body));

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(event)));

    // then
    assertTrue(event.getFragment().getBody()
        .startsWith("<!-- data-knotx-id=\"" + event.getFragment().getId() + "\" -->"));
    assertTrue(event.getFragment().getBody()
        .endsWith("<!-- data-knotx-id=\"" + event.getFragment().getId() + "\" -->"));
  }

  @Test
  @DisplayName("Expect fragment body contains debug script when fragment type configured.")
  void expectFragmentBodyContainsDebugScript() {
    //given
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);

    JsonObject expectedLog = FragmentExecutionLog.newInstance(event).toJson();

    String scriptRegexp =
        "<script data-knotx-debug=\"log\" data-knotx-id=\"" + event.getFragment().getId()
            + "\" type=\"application/json\">(?<fragmentEventJson>.*?)</script>";
    Pattern scriptPattern = Pattern.compile(scriptRegexp, Pattern.DOTALL);

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(event)));

    // then
    Matcher matcher = scriptPattern.matcher(event.getFragment().getBody());
    assertTrue(matcher.find());
    assertJsonEquals(expectedLog,
        new FragmentExecutionLog(new JsonObject(matcher.group("fragmentEventJson"))).toJson());
  }

  @Test
  @DisplayName("Expect log debug script is a first HTML tag.")
  void expectLogDebugScriptAfterComment() {
    //given
    String body = "<div>body</div>";
    FragmentEvent event = new FragmentEvent(new Fragment("snippet", new JsonObject(), body));

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(event)));

    // then
    String bodyWithoutComments = event.getFragment().getBody()
        .replaceAll("<!-- data-knotx-id=\"" + event.getFragment().getId() + "\" -->", "");
    assertTrue(
        bodyWithoutComments.startsWith(
            "<script data-knotx-debug=\"log\" data-knotx-id=\"" + event.getFragment().getId()
                + "\" type=\"application/json\">"));
  }

  @Test
  @DisplayName("Expect debug script is a first HTML tag.")
  void expectGraphDebugScriptAfterComment() {
    //given
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);
    GraphNodeExecutionLog graphNodeExecutionLog = new GraphNodeExecutionLog().setId("root-node-id");

    JsonObject expectedLog = new JsonObject()
        .put("startTime", 0)
        .put("finishTime", 0)
        .put("status", NodeStatus.UNPROCESSED)
        .put("fragment", new JsonObject()
            .put("id", fragment.getId())
            .put("type", "snippet"))
        .put("graph", new JsonObject()
            .put("id", "root-node-id")
            .put("status", NodeStatus.UNPROCESSED));

    // when
    FragmentExecutionLogConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
            .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));
    tested.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        ImmutableList.of(FragmentExecutionLog.newInstance(event, graphNodeExecutionLog)));

    // then
    String bodyWithoutComments = event.getFragment().getBody()
        .replaceAll("<!-- data-knotx-id=\"" + event.getFragment().getId() + "\" -->", "");

    Pattern secondTagsContent = Pattern.compile(
        "<script data-knotx-debug=\"log\" data-knotx-id=\"" + event
            .getFragment().getId()
            + "\" type=\"application/json\">(.*)</script>.*", Pattern.DOTALL);

    Matcher matcher = secondTagsContent.matcher(bodyWithoutComments);

    assertTrue(matcher.matches());
    JsonObject output = new JsonObject(matcher.group(1));
    assertJsonEquals(expectedLog, output);
  }
}
