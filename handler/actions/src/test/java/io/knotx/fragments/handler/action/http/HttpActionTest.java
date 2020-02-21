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
package io.knotx.fragments.handler.action.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.knotx.fragments.handler.action.http.response.EndpointResponseProcessor.TIMEOUT_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.action.http.options.EndpointOptions;
import io.knotx.fragments.handler.action.http.options.HttpActionOptions;
import io.knotx.fragments.handler.action.http.options.ResponseOptions;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.knotx.fragments.engine.api.node.single.FragmentContext;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.knotx.fragments.handler.api.domain.payload.ActionPayload;
import io.knotx.fragments.handler.api.domain.payload.ActionRequest;
import io.knotx.fragments.handler.api.domain.payload.ActionResponse;
import io.knotx.fragments.handler.api.domain.payload.ActionResponseError;
import io.knotx.server.api.context.ClientRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class HttpActionTest {

  private static final String VALID_REQUEST_PATH = "/valid-service";
  private static final String VALID_JSON_RESPONSE_BODY = "{ \"data\": \"service response\"}";
  private static final String VALID_JSON_ARRAY_RESPONSE_BODY = "[ \"first service response\", \" second service response\"]";
  private static final String VALID_EMPTY_RESPONSE_BODY = "";
  private static final String ACTION_ALIAS = "httpAction";
  private static final String RAW_BODY = "<html>Hello</html>";
  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_TEXT = "application/text";
  private static final String JSON = "JSON";
  private static final String NOT_EXISTING_PREDICATE = "not existing predicate";
  private static final String JSON_BODY = "{\n" +
      "  \"id\": 21762532,\n" +
      "  \"url\": \"http://knotx.io\",\n" +
      "  \"label\": \"Product\"\n" +
      "}";
  private static final Set<String> ALLOW_ALL_HEADERS = Collections.singleton(".*");
  private static final JsonObject EMPTY_JSON = new JsonObject();

  private WireMockServer wireMockServer;
  private ActionLogLevel actionLogLevel = ActionLogLevel.INFO;

  static Stream<Arguments> dataExpectSuccessTransitionAndJsonBody() {
    return Stream.of( //Content-Type, forceJson, JSON predicate, Body,
        Arguments.of(APPLICATION_JSON, false, null, JSON_BODY),
        Arguments.of(APPLICATION_TEXT, true, null, JSON_BODY),
        Arguments.of(APPLICATION_JSON, false, JSON, JSON_BODY)
    );
  }

  static Stream<Arguments> dataExpectSuccessTransitionAndTextBody() {
    return Stream.of( //Content-Type, forceJson, JSON predicate, Body
        Arguments.of(APPLICATION_TEXT, false, null, JSON_BODY)
    );
  }

  static Stream<Arguments> dataExpectErrorTransitionAndNullBody() {
    return Stream.of( //Content-Type, forceJson, JSON predicate, Body
        Arguments.of(APPLICATION_JSON, false, null, RAW_BODY),
        Arguments.of(APPLICATION_TEXT, true, null, RAW_BODY),
        Arguments.of(APPLICATION_JSON, false, JSON, RAW_BODY)
    );
  }

  static Stream<Arguments> dataExpectExceptionAndNullBody() {
    return Stream.of(
        Arguments.of(APPLICATION_TEXT, false, JSON, JSON_BODY),
        Arguments.of(APPLICATION_TEXT, true, JSON, JSON_BODY)
    );
  }

  @BeforeEach
  void setUp() {
    this.wireMockServer = new WireMockServer(options().dynamicPort());
    this.wireMockServer.start();
  }

  @AfterEach
  void tearDown() {
    this.wireMockServer.stop();
  }

  @Test
  @DisplayName("Expect success transition when endpoint returned success status code")
  void expectSuccessTransitionWhenSuccessResponse(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect action alias key in fragment payload when endpoint responded with success status code")
  void appendActionAliasToPayload(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertTrue(
            fragmentResult.getFragment()
                .getPayload()
                .containsKey(ACTION_ALIAS)),
        testContext);
  }

  @Test
  @DisplayName("Expect fragment payload appended with endpoint result when endpoint responded with success status code and JSON body")
  void appendPayloadWhenEndpointResponseWithJsonObject(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      assertTrue(payload.getResponse()
          .isSuccess());
      assertEquals(VALID_JSON_RESPONSE_BODY, payload.getResult());
    }, testContext);
  }

  @Test
  @DisplayName("Expect fragment payload appended with endpoint result when endpoint responded with success status code and JSONArray body")
  void appendPayloadWhenEndpointResponseWithJsonArrayVertxTestContext(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_ARRAY_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      assertTrue(payload.getResponse()
          .isSuccess());
      assertEquals(VALID_JSON_ARRAY_RESPONSE_BODY, payload.getResult());
    }, testContext);
  }

  @Test
  @DisplayName("Expect fragment's body not modified when endpoint responded with OK and empty body")
  void fragmentsBodyNotModifiedWhenEmptyResponseBody(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_EMPTY_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(createFragment().getBody(), fragmentResult.getFragment()
            .getBody()),
        testContext);
  }

  @Test
  @DisplayName("Expect response metadata in payload when endpoint returned success status code")
  void responseMetadataInPayloadWhenSuccessResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionResponse response = payload.getResponse();
      assertNotNull(response);
      assertTrue(response.isSuccess());
      JsonObject metadata = response.getMetadata();
      assertNotNull(metadata);
      assertEquals("200", metadata.getString("statusCode"));
      assertEquals(new JsonArray().add("response"),
          metadata.getJsonObject("headers")
              .getJsonArray("responseHeader"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect response metadata in payload when endpoint returned error status code")
  void responseMetadataInPayloadWhenErrorResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = errorAction(vertx, 500, "Internal Error");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionResponse response = payload.getResponse();
      assertFalse(response.isSuccess());
      ActionResponseError error = response.getError();
      assertNotNull(error);
      assertEquals("500 Internal Server Error", error.getCode());
      assertEquals("Internal Error", error.getMessage());
      JsonObject metadata = response.getMetadata();
      assertNotNull(metadata);
      assertEquals("500", metadata.getString("statusCode"));
      assertEquals(new JsonArray().add("response"),
          metadata.getJsonObject("headers")
              .getJsonArray("responseHeader"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect request metadata in payload when endpoint returned success status code")
  void requestMetadataInPayloadWhenSuccessResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionRequest request = payload.getRequest();
      assertNotNull(request);
      assertEquals("HTTP", request.getType());
      assertEquals(VALID_REQUEST_PATH, request.getSource());
      JsonObject metadata = request.getMetadata();
      assertNotNull(metadata);
      assertEquals(new JsonArray().add("request"),
          metadata.getJsonObject("headers")
              .getJsonArray("requestHeader"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect request metadata in payload when endpoint returned error status code")
  void requestMetadataInPayloadWhenErrorResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = errorAction(vertx, 500, "Internal Error");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionRequest request = payload.getRequest();
      assertNotNull(request);
      assertEquals("HTTP", request.getType());
      assertEquals(VALID_REQUEST_PATH, request.getSource());
      JsonObject metadata = request.getMetadata();
      assertNotNull(metadata);
      assertEquals(new JsonArray().add("request"),
          metadata.getJsonObject("headers")
              .getJsonArray("requestHeader"));
    }, testContext);

  }

  @Test
  @DisplayName("Expect error transition when endpoint returned error status code")
  void errorTransitionWhenErrorStatusCode(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = errorAction(vertx, 500, "Internal Error");
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(ERROR_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @ParameterizedTest(name = "Expect success transition and response as JSON")
  @MethodSource("dataExpectSuccessTransitionAndJsonBody")
  void testSuccessTransitionsExpectedAndResponseAsJson(String contentType, boolean forceJson,
      String jsonPredicate, String responseBody, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    String endpointPath = "/api/success-json";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, actionLogLevel);

    verifyExecution(tested, clientRequest, new Fragment("type", new JsonObject(), "expectedBody"),
        fragmentResult -> {
          assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload()
              .getJsonObject("httpAction").getJsonObject("_result");
          assertEquals(new JsonObject()
              .put("id", 21762532)
              .put("url", "http://knotx.io")
              .put("label", "Product"), result);
        }, testContext);
  }

  @ParameterizedTest(name = "Expect success transition and response as raw text")
  @MethodSource("dataExpectSuccessTransitionAndTextBody")
  void testSuccessTransitionExpectedAndResponseAsRawText(String contentType, boolean forceJson,
      String jsonPredicate, String responseBody, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    String endpointPath = "/api/success-text";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, actionLogLevel);

    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
      String result = (String) fragmentResult.getFragment().getPayload().getJsonObject("httpAction")
          .getMap().get("_result");
      assertEquals(JSON_BODY, result);
    }, testContext);
  }

  @ParameterizedTest(name = "Expect _error transition and empty payload")
  @MethodSource("dataExpectErrorTransitionAndNullBody")
  void testErrorTransitionAndEmptyPayload(String contentType, boolean forceJson,
      String jsonPredicate, String responseBody, VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    String endpointPath = "/api/error-no-response";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, actionLogLevel);

    verifyExecution(tested, clientRequest, new Fragment("type", EMPTY_JSON, "expectedBody"),
        fragmentResult -> {
          assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload();
          assertEquals(EMPTY_JSON, result);
        }, testContext);
  }

  @ParameterizedTest(name = "Expect _error transition, empty payload and ReplyException")
  @MethodSource("dataExpectExceptionAndNullBody")
  void testExpectExceptionAndNoResponse(String contentType, boolean forceJson, String jsonPredicate,
      String responseBody, VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/exception-no-response";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, actionLogLevel);

    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      assertNotNull(fragmentResult);
      assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
      assertEquals(EMPTY_JSON, fragmentResult.getFragment().getPayload());
      JsonObject logs = fragmentResult.getNodeLog().getJsonObject("logs");
      JsonObject exception = (JsonObject) logs.getJsonArray("errors").getList().get(0);
      assertEquals(ReplyException.class.getCanonicalName(), exception.getString("className"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect IllegalArgumentException when not existing predicate provided")
  void expectErrorWhenNotExistingPredicateProvided(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    String endpointPath = "/api/exception-wrong-predicate";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, JSON_BODY, APPLICATION_JSON,
        NOT_EXISTING_PREDICATE, false, actionLogLevel);

    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      assertNotNull(fragmentResult);
      assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
      assertEquals(EMPTY_JSON, fragmentResult.getFragment().getPayload());
      JsonObject logs = fragmentResult.getNodeLog().getJsonObject("logs");
      JsonObject exception = (JsonObject) logs.getJsonArray("errors").getList().get(0);
      assertEquals(IllegalArgumentException.class.getCanonicalName(),
          exception.getString("className"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect _error transition, empty payload and DecodeException when service responds with invalid json when json expected")
  void expectErrorTransitionAndEmptyPayloadWhenInvalidJsonProvided(VertxTestContext testContext,
      Vertx vertx)
      throws Throwable {
    String endpointPath = "/api/invalid-api-response";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, RAW_BODY, APPLICATION_JSON,
        null, false, ActionLogLevel.INFO);

    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      assertNotNull(fragmentResult);
      assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
      assertEquals(EMPTY_JSON, fragmentResult.getFragment().getPayload());
      JsonObject logs = fragmentResult.getNodeLog().getJsonObject("logs");
      JsonObject exception = (JsonObject) logs.getJsonArray("errors").getList().get(0);
      assertEquals(DecodeException.class.getCanonicalName(), exception.getString("className"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect _timeout transition when endpoint times out")
  void errorTransitionWhenEndpointTimesOut(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given, when
    int requestTimeoutMs = 1000;
    wireMockServer.stubFor(get(urlEqualTo(VALID_REQUEST_PATH))
        .willReturn(aResponse().withFixedDelay(2 * requestTimeoutMs)));

    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setHeaders(MultiMap.caseInsensitiveMultiMap());

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(VALID_REQUEST_PATH)
        .setDomain("localhost")
        .setPort(wireMockServer.port());

    HttpAction tested = new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setRequestTimeoutMs(requestTimeoutMs)
            .setLogLevel(actionLogLevel.getLevel()),
        ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(TIMEOUT_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect error transition when calling not existing endpoint")
  void errorTransitionWhenEndpointDoesNotExist(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), "not-existing-endpoint");

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath("not-existing-endpoint")
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS);

    HttpAction tested = new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(ERROR_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect headers from FragmentContext clientRequest are filtered and sent in endpoint request")
  void headersFromClientRequestFilteredAndSendToEndpoint(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap()
        .add("crHeaderKey", "crHeaderValue");
    HttpAction tested = getHttpActionWithAdditionalHeaders(vertx,
        null, "crHeaderKey", "crHeaderValue");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect additionalHeaders from EndpointOptions are sent in endpoint request")
  void additionalHeadersSentToEndpoint(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap();
    JsonObject additionalHeaders = new JsonObject().put("additionalHeader", "additionalValue");
    HttpAction tested = getHttpActionWithAdditionalHeaders(vertx,
        additionalHeaders, "additionalHeader", "additionalValue");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);

  }

  @Test
  @DisplayName("Expect additionalHeaders override headers from FragmentContext clientRequest")
  void additionalHeadersOverrideClientRequestHeaders(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap()
        .add("customHeader", "crHeaderValue");
    JsonObject additionalHeaders = new JsonObject().put("customHeader", "additionalValue");
    HttpAction tested = getHttpActionWithAdditionalHeaders(vertx,
        additionalHeaders, "customHeader", "additionalValue"
    );
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, HttpActionTest.VALID_REQUEST_PATH);
    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest headers")
  void placeholdersInPathResolvedWithHeadersValues(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap()
        .add("bookId", "999000");
    String endpointPath = "/api/book/999000";
    String clientRequestPath = "/book-page";
    String optionsPath = "/api/book/{header.bookId}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS);

    HttpAction tested = new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest query params")
  void placeholdersInPathResolvedWithClientRequestQueryParams(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestParams = MultiMap.caseInsensitiveMultiMap()
        .add("bookId", "999000");
    String endpointPath = "/api/book/999000";
    String clientRequestPath = "/book-page";
    String optionsPath = "/api/book/{param.bookId}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(clientRequestParams,
        MultiMap.caseInsensitiveMultiMap(), clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS);

    HttpAction tested = new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest request uri")
  void placeholdersInPathResolvedWithClientRequesUriParams(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/thumbnail.png";
    String clientRequestPath = "/book.png";
    String optionsPath = "/api/thumbnail.{uri.extension}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS);

    HttpAction tested = new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, createFragment(),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in payload resolved with values from FragmentContext clientRequest request uri")
  void placeholdersInPayloadResolvedWithClientRequesUriParams(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/thumbnail.png";
    String clientRequestPath = "/book.png";
    String optionsPath = "/api/thumbnail.{payload.thumbnail.extension}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS);

    HttpAction tested = new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest,
        createFragment().appendPayload("thumbnail", new JsonObject().put("extension", "png")),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  private HttpAction successAction(Vertx vertx, String responseBody) {
    return getHttpAction(vertx, HttpActionTest.VALID_REQUEST_PATH, responseBody,
        HttpResponseStatus.OK.code(), null);
  }

  private HttpAction errorAction(Vertx vertx, int statusCode, String statusMessage) {
    return getHttpAction(vertx, HttpActionTest.VALID_REQUEST_PATH, null, statusCode, statusMessage);
  }

  private HttpAction getHttpAction(Vertx vertx, String requestPath, String responseBody,
      int statusCode, String statusMessage) {
    wireMockServer.stubFor(get(urlEqualTo(requestPath))
        .willReturn(aResponse()
            .withHeader("responseHeader", "response")
            .withBody(responseBody)
            .withStatus(statusCode)
            .withStatusMessage(statusMessage)));

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(requestPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(Collections.singleton("requestHeader"));

    return new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);
  }

  private HttpAction getHttpActionWithAdditionalHeaders(Vertx vertx,
      JsonObject additionalHeaders, String expectedHeaderKey, String expectedHeaderValue) {
    wireMockServer.stubFor(get(urlEqualTo(HttpActionTest.VALID_REQUEST_PATH))
        .withHeader(expectedHeaderKey, matching(expectedHeaderValue))
        .willReturn(aResponse()
            .withBody(VALID_JSON_RESPONSE_BODY)));

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(HttpActionTest.VALID_REQUEST_PATH)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS)
        .setAdditionalHeaders(additionalHeaders);

    return new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions().setEndpointOptions(endpointOptions)
            .setLogLevel(actionLogLevel.getLevel()), ACTION_ALIAS);
  }

  private HttpAction setupTestingInstances(Vertx vertx, String endpointPath, String body,
      String contentType, String jsonPredicate, boolean forceJson, ActionLogLevel logLevel) {
    Set<String> predicates = new HashSet<>();
    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(body)
            .withHeader("Content-Type", contentType)));

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(ALLOW_ALL_HEADERS);

    Optional.ofNullable(jsonPredicate).ifPresent(predicates::add);
    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(forceJson);

    return new HttpAction(createDefaultWebClient(vertx),
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setResponseOptions(responseOptions)
            .setLogLevel(logLevel.getLevel()),
        ACTION_ALIAS);
  }

  private void verifyExecution(HttpAction tested, ClientRequest clientRequest, Fragment fragment,
      Consumer<FragmentResult> successAssertions,
      VertxTestContext testContext) throws Throwable {
    tested.apply(new FragmentContext(fragment, clientRequest),
        testContext.succeeding(result -> {
          testContext.verify(() -> successAssertions.accept(result));
          testContext.completeNow();
        }));
    //then
    assertTrue(testContext.awaitCompletion(60, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private ClientRequest prepareClientRequest(MultiMap clientRequestParams,
      MultiMap headers, String clientRequestPath) {
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setPath(clientRequestPath);
    clientRequest.setHeaders(headers);
    clientRequest.setParams(clientRequestParams);
    return clientRequest;
  }

  private Fragment createFragment() {
    return new Fragment("type", EMPTY_JSON, "expectedBody");
  }

  private WebClient createDefaultWebClient(Vertx vertx) {
    return WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx), new WebClientOptions());
  }
}
