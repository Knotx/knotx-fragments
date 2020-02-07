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

import static io.knotx.fragments.handler.consumer.FragmentHtmlBodyWriterFactory.CONDITION_OPTION;
import static io.knotx.fragments.handler.consumer.FragmentHtmlBodyWriterFactory.FRAGMENT_TYPES_OPTIONS;
import static io.knotx.fragments.handler.consumer.FragmentHtmlBodyWriterFactory.HEADER_OPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.TasksWithFragmentEvents;
import io.knotx.fragments.task.factory.node.CompositeNodeWithMetadata;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
import io.knotx.fragments.task.factory.node.SingleNodeWithMetadata;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentHtmlBodyWriterFactoryTest {

  public static final String EXPECTED_FRAGMENT_TYPE = "snippet";

  public static final String EXPECTED_HEADER = "x-knotx-debug";

  public static final String EXPECTED_PARAM = "debug";

  public static final FragmentContext DUMMY_FRAGMENT_CONTEXT = new FragmentContext(new Fragment("fragment",
      new JsonObject(), ""), new ClientRequest());

  private static final String PARAM_OPTION = "param";

  public static final FragmentEventsConsumer CONFIGURED_FACTORY = new FragmentHtmlBodyWriterFactory().create(new JsonObject()
      .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
      .put(CONDITION_OPTION, new JsonObject().put(PARAM_OPTION, EXPECTED_PARAM)));

  private static final Action DUMMY_ACTION = (fragmentContext, resultHandler) -> {
  };

  @Test
  @DisplayName("Expect fragment is not modified when condition not configured")
  void expectFragmentNotModifiedWhenConditionNotConfigured() {
    // given
    Task<NodeWithMetadata> task = new Task<>("testTask");
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentEventsConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject().put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(
            EXPECTED_FRAGMENT_TYPE)));
    tested.accept(new ClientRequest(), new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(original)));

    // then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is not modified when supported fragments types not configured.")
  void expectFragmentNotModifiedWhenSupportedTypesNotConfigured() {
    // given
    Task<NodeWithMetadata> task = new Task<>("testTask");
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentEventsConsumer tested = new FragmentHtmlBodyWriterFactory()
        .create(new JsonObject()
            .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(original)));

    // then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is not modified when supported fragments does not contain fragment type.")
  void expectFragmentNotModifiedWhenOtherSupportedTypeConfigured() {
    // given
    Task<NodeWithMetadata> task = new Task<>("testTask");
    FragmentEvent original = new FragmentEvent(new Fragment("json", new JsonObject(),
        "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentEventsConsumer tested = new FragmentHtmlBodyWriterFactory().create(new JsonObject()
        .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
        .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(original)));

    // then
    assertEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is modified when header condition and supported type configured.")
  void expectFragmentBodyModifiedWhenHeaderConditionConfigured() {
    // given
    Task<NodeWithMetadata> task = new Task<>("testTask");
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    FragmentEventsConsumer tested = new FragmentHtmlBodyWriterFactory().create(new JsonObject()
        .put(FRAGMENT_TYPES_OPTIONS, new JsonArray().add(EXPECTED_FRAGMENT_TYPE))
        .put(CONDITION_OPTION, new JsonObject().put(HEADER_OPTION, EXPECTED_HEADER)));
    tested.accept(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_HEADER, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(original)));

    // then
    // then
    assertNotEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment is modified when param condition and supported type configured.")
  void expectFragmentBodyModifiedWhenParamConditionConfigured() {
    // given
    Task<NodeWithMetadata> task = new Task<>("testTask");
    FragmentEvent original = new FragmentEvent(
        new Fragment(EXPECTED_FRAGMENT_TYPE, new JsonObject(),
            "{ \"body\": \"<div>body</div>\" }"));
    FragmentEvent copy = new FragmentEvent(original.toJson());

    // when
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(original)));

    // then
    assertNotEquals(copy, original);
  }

  @Test
  @DisplayName("Expect fragment body is wrapped by fragmentId.")
  void expectFragmentBodyWrappedByFragmentId() {
    // given
    String body = "<div>body</div>";
    Task<NodeWithMetadata> task = new Task<>("testTask");
    FragmentEvent event = new FragmentEvent(new Fragment("snippet", new JsonObject(), body));

    // when
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

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
    Task<NodeWithMetadata> task = new Task<>("testTask");
    String body = "<div>body</div>";
    FragmentEvent event = new FragmentEvent(new Fragment("snippet", new JsonObject(), body));
    JsonObject eventData = event.toJson();

    String scriptRegexp = "<script data-knotx-debug=\"log\" data-knotx-id=\"" + event.getFragment().getId()
        + "\" type=\"application/json\">(?<fragmentEventJson>.*?)</script>";
    Pattern scriptPattern = Pattern.compile(scriptRegexp, Pattern.DOTALL);

    // when
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    Matcher matcher = scriptPattern.matcher(event.getFragment().getBody());
    assertTrue(matcher.find());
    assertEquals(eventData, new JsonObject(matcher.group("fragmentEventJson")));
  }

  @Test
  @DisplayName("Expect debug script is first HTML tag.")
  void expectDebugScriptAfterComment() {
    //given
    Task<NodeWithMetadata> task = new Task<>("testTask");
    String body = "<div>body</div>";
    FragmentEvent event = new FragmentEvent(new Fragment("snippet", new JsonObject(), body));

    // when
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    String bodyWithoutComments = event.getFragment().getBody()
        .replaceAll("<!-- data-knotx-id=\"" + event.getFragment().getId() + "\" -->", "");
    assertTrue(
        bodyWithoutComments.startsWith("<script data-knotx-debug=\"log\" data-knotx-id=\"" + event.getFragment().getId()
            + "\" type=\"application/json\">"));
  }

  @Test
  @DisplayName("Expect a graph with two successful simple nodes.")
  void expectFragmentBodyContainsDebugGraphDataWhenSuccess() throws IOException {
    //given
    SingleNodeWithMetadata childNode = new SingleNodeWithMetadata("childNode",
        transitTo("_success", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata rootNode = new SingleNodeWithMetadata("someNode",
        transitTo("_success", new JsonObject().put("sample", "node Log")), ImmutableMap.of("_success", childNode),
        "someOtherFactory");
    Task<NodeWithMetadata> task = new Task<>("testTask", rootNode);
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);
    FragmentContext fragmentContext = new FragmentContext(fragment, new ClientRequest());

    JsonObject expected = readFromResources("handler/consumer/graph/twoNodesSuccess.json");

    // when
    rootNode.execute(fragmentContext).blockingGet();
    childNode.execute(fragmentContext).blockingGet();
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    JsonObject actual = retrieveGraphData(event);
    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("Expect a graph with error transition.")
  void expectFragmentBodyContainsDebugGraphDataWhenErrorTransition() throws IOException {
    //given
    SingleNodeWithMetadata successChild = new SingleNodeWithMetadata("successChild",
        transitTo("_success", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata failChild = new SingleNodeWithMetadata("failChild",
        transitTo("_success", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata rootNode = new SingleNodeWithMetadata("someNode",
        transitTo("_error", new JsonObject().put("sample", "node Log")),
        ImmutableMap.of("_success", successChild,
            "_error", failChild),
        "someOtherFactory");
    Task<NodeWithMetadata> task = new Task<>("testTask", rootNode);
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);
    FragmentContext fragmentContext = new FragmentContext(fragment, new ClientRequest());

    JsonObject expected = readFromResources("handler/consumer/graph/threeNodesError.json");

    // when
    rootNode.execute(fragmentContext).blockingGet();
    failChild.execute(fragmentContext).blockingGet();
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    JsonObject actual = retrieveGraphData(event);
    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("Expect a graph with custom transitions.")
  void expectFragmentBodyContainsDebugGraphDataWhenCustomTransition() throws IOException {
    //given
    SingleNodeWithMetadata successChild = new SingleNodeWithMetadata("successChild",
        transitTo("_success", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata customChild = new SingleNodeWithMetadata("customChild",
        transitTo("someothercustomtransition", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata rootNode = new SingleNodeWithMetadata("someNode",
        transitTo("customtransition", new JsonObject().put("sample", "node Log")),
        ImmutableMap.of("_success", successChild,
            "customtransition", customChild),
        "someOtherFactory");
    Task<NodeWithMetadata> task = new Task<>("testTask", rootNode);
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);
    FragmentContext fragmentContext = new FragmentContext(fragment, new ClientRequest());

    JsonObject expected = readFromResources("handler/consumer/graph/threeNodesCustomTransitions.json");

    // when
    rootNode.execute(fragmentContext).blockingGet();
    customChild.execute(fragmentContext).blockingGet();
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    JsonObject actual = retrieveGraphData(event);
    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("Expect a graph with composite nodes.")
  void expectFragmentBodyContainsDebugGraphDataWhenCompositeNode() throws IOException {
    //given
    SingleNodeWithMetadata successChild = new SingleNodeWithMetadata("successChild",
        transitTo("_success", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata customChild = new SingleNodeWithMetadata("customChild",
        transitTo("someothercustomtransition", new JsonObject()), new HashMap<>(), "someFactory");
    CompositeNodeWithMetadata rootNode = new CompositeNodeWithMetadata("rootNode",
        ImmutableList.of(successChild, customChild),
        new HashMap<>());
    Task<NodeWithMetadata> task = new Task<>("testTask", rootNode);
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);
    FragmentContext fragmentContext = new FragmentContext(fragment, new ClientRequest());

    JsonObject expected = readFromResources("handler/consumer/graph/compositeNode.json");

    // when
    customChild.execute(fragmentContext).blockingGet();
    rootNode.next("_success");
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    JsonObject actual = retrieveGraphData(event);
    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("Expect a graph with single and composite nodes.")
  void expectFragmentBodyContainsDebugGraphDataWhenSingleAndCompositeNode() throws IOException {
    //given
    SingleNodeWithMetadata successChild = new SingleNodeWithMetadata("successChild",
        transitTo("_success", new JsonObject()), new HashMap<>(), "someFactory");
    SingleNodeWithMetadata customChild = new SingleNodeWithMetadata("customChild",
        transitTo("someothercustomtransition", new JsonObject()), new HashMap<>(), "someFactory");
    CompositeNodeWithMetadata compositeNode = new CompositeNodeWithMetadata("compositeNode",
        ImmutableList.of(successChild, customChild),
        new HashMap<>());
    SingleNodeWithMetadata rootNode = new SingleNodeWithMetadata("someNode",
        transitTo("_success", new JsonObject().put("sample", "node Log")),
        ImmutableMap.of("_success", compositeNode),
        "someOtherFactory");
    Task<NodeWithMetadata> task = new Task<>("testTask", rootNode);
    String body = "<div>body</div>";
    Fragment fragment = new Fragment("snippet", new JsonObject(), body);
    FragmentEvent event = new FragmentEvent(fragment);
    FragmentContext fragmentContext = new FragmentContext(fragment, new ClientRequest());

    JsonObject expected = readFromResources("handler/consumer/graph/compositeNode.json");

    // when
    rootNode.execute(fragmentContext).blockingGet();
    customChild.execute(fragmentContext).blockingGet();
    compositeNode.next("_success");
    CONFIGURED_FACTORY.accept(new ClientRequest()
            .setParams(MultiMap.caseInsensitiveMultiMap().add(EXPECTED_PARAM, "true")),
        new TasksWithFragmentEvents(ImmutableList.of(task), ImmutableList.of(event)));

    // then
    JsonObject actual = retrieveGraphData(event);
    assertEquals(expected, actual);
  }

  private JsonObject readFromResources(String fileName) throws IOException {
    return new JsonObject(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(fileName), StandardCharsets.UTF_8));
  }

  private static JsonObject retrieveGraphData(FragmentEvent event) {
    String scriptRegexp = "<script data-knotx-debug=\"graph\" data-knotx-id=\"" + event.getFragment().getId()
        + "\" type=\"application/json\">(?<fragmentEventJson>.*?)</script>";
    Pattern scriptPattern = Pattern.compile(scriptRegexp, Pattern.DOTALL);
    Matcher matcher = scriptPattern.matcher(event.getFragment().getBody());
    assertTrue(matcher.find());
    String fragmentEventJson = matcher.group("fragmentEventJson");
    return new JsonObject(fragmentEventJson);
  }

  private static Action transitTo(String transition, JsonObject nodeLog) {
    return (fragmentContext, resultHandler) -> {
      Future.succeededFuture(new FragmentResult(fragmentContext.getFragment(), transition, nodeLog))
          .setHandler(resultHandler);
    };
  }
}