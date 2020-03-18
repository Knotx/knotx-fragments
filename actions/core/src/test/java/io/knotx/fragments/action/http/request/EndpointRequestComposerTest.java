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
package io.knotx.fragments.action.http.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.action.http.options.EndpointOptions;
import io.knotx.server.api.context.ClientRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EndpointRequestComposerTest {

  private static final String INTERPOLATED_VALUE = "interpolated-value";
  private static final String SAMPLE_BODY_STRING = "sample-body";
  private static final String SAMPLE_BODY_STRING_WITH_PLACEHOLDER = "{payload.action._result}";
  private static final JsonObject SAMPLE_BODY_JSON = new JsonObject()
      .put("key", "value")
      .put("otherKey", new JsonObject().put("nestedKey", "nestedValue"));
  private static final JsonObject SAMPLE_BODY_JSON_WITH_PLACEHOLDER = new JsonObject()
      .put("key", "value")
      .put("otherKey", new JsonObject().put("nestedKey", "{payload.action._result}"));
  private static final JsonObject SAMPLE_BODY_JSON_INTERPOLATED = new JsonObject()
      .put("key", "value")
      .put("otherKey", new JsonObject().put("nestedKey", INTERPOLATED_VALUE));
  private static final JsonObject SAMPLE_PAYLOAD = new JsonObject().put("action", new JsonObject().put("_result", INTERPOLATED_VALUE));

  private EndpointRequestComposer tested;

  @Test
  @DisplayName("Expect extra header when bodyJson specified and Content-Type header not specified")
  void shouldContainExtraContentTypeHeader() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("bodyJson", SAMPLE_BODY_JSON);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext());

    assertEquals(HttpHeaderValues.APPLICATION_JSON.toString(),
        result.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
  }

  @Test
  @DisplayName("Expect unchanged header when bodyJson specified and Content-Type header taken from ClientRequest")
  void shouldContainUnchangedContentTypeHeaderFromClientRequest() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("bodyJson", SAMPLE_BODY_JSON)
        .put("allowedRequestHeaders", new JsonArray().add(HttpHeaderNames.CONTENT_TYPE.toString()));

    givenComposer(configuration);

    EndpointRequest result = tested
        .createEndpointRequest(sampleFragmentContext(HttpHeaderValues.TEXT_PLAIN.toString()));

    assertEquals(HttpHeaderValues.TEXT_PLAIN.toString(),
        result.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
  }

  @Test
  @DisplayName("Expect unchanged header when bodyJson specified and Content-Type header specified in configuration")
  void shouldContainUnchangedContentTypeHeaderFromConfiguration() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("bodyJson", SAMPLE_BODY_JSON)
        .put("additionalHeaders", new JsonObject()
            .put(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.TEXT_PLAIN));

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext());

    assertEquals(HttpHeaderValues.TEXT_PLAIN.toString(),
        result.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
  }

  @Test
  @DisplayName("Expect body as string when provided in configuration")
  void shouldContainBodyString() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("body", SAMPLE_BODY_STRING);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext());

    assertEquals(SAMPLE_BODY_STRING, result.getBody());
  }

  @Test
  @DisplayName("Expect body as json when provided in configuration")
  void shouldContainBodyJson() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("bodyJson", SAMPLE_BODY_JSON);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext());

    assertEquals(SAMPLE_BODY_JSON.toString(), result.getBody());
  }

  @Test
  @DisplayName("Expect exception when both body and bodyJson specified")
  void shouldThrowWhenBothBodyOptionsSpecified() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("body", SAMPLE_BODY_STRING)
        .put("bodyJson", SAMPLE_BODY_JSON);

    EndpointOptions options = new EndpointOptions(configuration);

    assertThrows(IllegalArgumentException.class, () -> new EndpointRequestComposer(options));
  }

  @Test
  @DisplayName("Expect that body is left as-is when body interpolation is disabled by default")
  void shouldNotReplacePlaceholdersInBodyByDefault() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("body", SAMPLE_BODY_STRING_WITH_PLACEHOLDER);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext());

    assertEquals(SAMPLE_BODY_STRING_WITH_PLACEHOLDER, result.getBody());
  }

  @Test
  @DisplayName("Expect interpolate body when body interpolation is enabled")
  void shouldReplacePlaceholdersInBodyWhenFlagSet() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("body", SAMPLE_BODY_STRING_WITH_PLACEHOLDER)
        .put("interpolateBody", true);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext(SAMPLE_PAYLOAD));

    assertEquals(INTERPOLATED_VALUE, result.getBody());
  }

  @Test
  @DisplayName("Expect that body JSON is left as-is when body interpolation is disabled")
  void shouldNotReplacePlaceholdersInBodyJsonWhenInterpolationDisabled() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("bodyJson", SAMPLE_BODY_JSON_WITH_PLACEHOLDER)
        .put("interpolateBody", false);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext(SAMPLE_PAYLOAD));

    assertEquals(SAMPLE_BODY_JSON_WITH_PLACEHOLDER.toString(), result.getBody());
  }

  @Test
  @DisplayName("Expect interpolated JSON body when body interpolation is enabled")
  void shouldReplacePlaceholdersInJsonBodyWhenFlagSet() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home")
        .put("domain", "google.com")
        .put("port", 80)
        .put("bodyJson", SAMPLE_BODY_JSON_WITH_PLACEHOLDER)
        .put("interpolateBody", true);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext(SAMPLE_PAYLOAD));

    assertEquals(SAMPLE_BODY_JSON_INTERPOLATED.toString(), result.getBody());
  }

  @Test
  @DisplayName("Expect interpolated path by default")
  void shouldInterpolatePathByDefault() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home/{payload.action._result}")
        .put("domain", "google.com")
        .put("port", 80);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext(SAMPLE_PAYLOAD));

    assertEquals("/home/" + INTERPOLATED_VALUE, result.getPath());
  }

  @Test
  @DisplayName("Expect path left as-is when path interpolation disabled")
  void shouldNotInterpolatePathWhenDisabled() {
    JsonObject configuration = new JsonObject()
        .put("path", "/home/{payload.action._result}")
        .put("domain", "google.com")
        .put("port", 80)
        .put("interpolatePath", false);

    givenComposer(configuration);

    EndpointRequest result = tested.createEndpointRequest(sampleFragmentContext(SAMPLE_PAYLOAD));

    assertEquals("/home/{payload.action._result}", result.getPath());
  }

  private void givenComposer(JsonObject configuration) {
    tested = new EndpointRequestComposer(new EndpointOptions(configuration));
  }

  private FragmentContext sampleFragmentContext() {
    return new FragmentContext(
        new Fragment("snippet", new JsonObject(), ""),
        new ClientRequest()
    );
  }

  private FragmentContext sampleFragmentContext(JsonObject payload) {
    return new FragmentContext(
        new Fragment("snippet", new JsonObject(), "").mergeInPayload(payload),
        new ClientRequest()
    );
  }

  private FragmentContext sampleFragmentContext(String contentType) {
    return new FragmentContext(
        new Fragment("snippet", new JsonObject(), ""),
        new ClientRequest().setHeaders(
            MultiMap.caseInsensitiveMultiMap().add(HttpHeaderNames.CONTENT_TYPE, contentType))
    );
  }

}
