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

package io.knotx.fragments.action.library.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.knotx.fragments.action.library.http.options.EndpointOptions;
import io.knotx.fragments.action.library.http.options.HttpActionOptions;
import io.knotx.fragments.action.library.http.request.EndpointRequest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(VertxExtension.class)
@Timeout(value = 60, timeUnit = SECONDS)
class EndpointInvokerIntegrationTest {

  private static Stream<Arguments> headersWithSpecialCharsInValues() {
    return Stream.of( // headerName, headerValue
        Arguments.of("Content", "param: value1; param: value2_*(24&1)"),
        Arguments.of("Content", new JsonObject()
            .put("param", "value")
            .put("ids", new JsonArray().add(1).add(2).add(3))
            .toString()
        )
    );
  }

  private static Stream<Arguments> headersWithIllegalCharsInNames() {
    return Stream.of( // headerName, headerValue
        Arguments.of("Content Type", "param: value;"),
        Arguments.of("Content:Type", "param: value;"),
        Arguments.of("Content\nType", "param: value;"),
        Arguments.of("Content;Type", "param: value;")
    );
  }

  private WireMockServer server;

  private EndpointInvoker tested;

  @BeforeEach
  void setUp(Vertx vertx) {
    WebClient webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx));
    server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    tested = new EndpointInvoker(webClient, createOptions());
  }

  @AfterEach
  void tearDown() {
    server.stop();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/products?fq_style=\"Hats, Scarves & Gloves\"&fq_attr=\"Size^8\"",
      "/test test/",
      "/some&%%/path"
  })
  @DisplayName("Expect 400 Bad Request response for path that is not encoded by EndpointInvoker")
  void shouldNotEncodePath(String path, VertxTestContext testContext) {
    // given
    serverRespondingOn(path);

    EndpointRequest endpointRequest = new EndpointRequest(path, noHeaders());

    // when, then
    expectResponse(testContext, endpointRequest,
        response -> assertEquals(400, response.statusCode()));
  }

  @ParameterizedTest
  @MethodSource("headersWithSpecialCharsInValues")
  @DisplayName("Expect 200 response for headers values that are not encoded by EndpointInvoker")
  void shouldAcceptNotEncodedHeaderValues(String headerName, String headerValue,
      VertxTestContext testContext) {
    // given
    serverRespondingGiven(headerName, headerValue);

    EndpointRequest request = new EndpointRequest("/", oneHeader(headerName, headerValue));

    // when, then
    expectResponse(testContext, request,
        response -> assertEquals(200, response.statusCode()));
  }

  @ParameterizedTest
  @MethodSource("headersWithIllegalCharsInNames")
  @DisplayName("Expect exception raised by WebClient when header name is illegal")
  void shouldNotAllowIllegalHeaders(String headerName, String headerValue,
      VertxTestContext testContext) {
    // given
    EndpointRequest request = new EndpointRequest("/", oneHeader(headerName, headerValue));

    // when, then
    expectError(testContext, request,
        error -> assertEquals(IllegalArgumentException.class, error.getClass()));
  }

  @Test
  @DisplayName("Expect WebClient to ignore header with empty name, but WireMockServer expecting it")
  void shouldIgnoreEmptyHeaderButServerExpects(VertxTestContext testContext) {
    // given
    serverRespondingGiven(StringUtils.EMPTY, "some-value");

    EndpointRequest request = new EndpointRequest("/", oneHeader(StringUtils.EMPTY, "some-value"));

    // when, then
    expectResponse(testContext, request,
        response -> assertEquals(404, response.statusCode()));
  }

  private void serverRespondingOn(String path) {
    server.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(200)));
  }

  private void serverRespondingGiven(String headerName, String headerValue) {
    server.stubFor(get(urlEqualTo("/")).withHeader(headerName, equalTo(headerValue))
        .willReturn(aResponse().withStatus(200)));
  }

  private HttpActionOptions createOptions() {
    EndpointOptions options = new EndpointOptions()
        .setDomain("localhost")
        .setPort(server.port());

    return new HttpActionOptions()
        .setEndpointOptions(options);
  }

  private MultiMap noHeaders() {
    return MultiMap.caseInsensitiveMultiMap();
  }

  private MultiMap oneHeader(String headerName, String headerValue) {
    return MultiMap.caseInsensitiveMultiMap().add(headerName, headerValue);
  }

  private void expectResponse(VertxTestContext testContext, EndpointRequest request,
      Consumer<HttpResponse<Buffer>> assertions) {
    tested.invokeEndpoint(request)
        .subscribe(result -> testContext.verify(() -> {
              assertions.accept(result);
              testContext.completeNow();
            }),
            testContext::failNow);
  }

  private void expectError(VertxTestContext testContext, EndpointRequest request,
      Consumer<Throwable> assertions) {
    tested.invokeEndpoint(request).subscribe(
        response -> testContext.failNow(new IllegalStateException(
            "Exception not thrown but expected")),
        error -> testContext.verify(() -> {
          assertions.accept(error);
          testContext.completeNow();
        })
    );
  }

}
