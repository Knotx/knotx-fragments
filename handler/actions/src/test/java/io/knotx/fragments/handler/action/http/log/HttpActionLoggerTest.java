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
package io.knotx.fragments.handler.action.http.log;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.knotx.fragments.handler.action.http.options.EndpointOptions;
import io.knotx.fragments.handler.action.http.request.EndpointRequest;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpActionLoggerTest {

  private static final String SAMPLE_ACTION_ALIAS = "TestedAction";
  private static final String REQUEST = "request";
  private static final String RESPONSE = "response";
  private static final String RESPONSE_BODY = "responseBody";
  private static final String ERRORS = "errors";

  private EndpointOptions endpointOptions;
  private EndpointRequest endpointRequest;

  @Mock
  private HttpResponse<Buffer> httpResponse;

  private HttpActionLogger tested;

  private static Stream<Arguments> happyPathCases() {
    return Stream.of( // logLevel, request, response, responseBody, error
        Arguments.of(ActionLogLevel.INFO, true, true, true, false),
        Arguments.of(ActionLogLevel.ERROR, false, false, false, false)
    );
  }

  private static Stream<Arguments> requestFailureCases() {
    return Stream.of( // logLevel, request, response, responseBody, error
        Arguments.of(ActionLogLevel.INFO, true, false, false, true),
        Arguments.of(ActionLogLevel.ERROR, true, false, false, true)
    );
  }

  private static Stream<Arguments> responseCodeTimeoutCases() {
    return Stream.of( // logLevel, request, response, responseBody, error
        Arguments.of(ActionLogLevel.INFO, true, false, false, true),
        Arguments.of(ActionLogLevel.ERROR, true, false, false, true)
    );
  }

  private static Stream<Arguments> responseCodeUnsuccessfulCases() {
    return Stream.of( // logLevel, request, response, responseBody, error
        Arguments.of(ActionLogLevel.INFO, true, true, false, true),
        Arguments.of(ActionLogLevel.ERROR, true, true, false, true)
    );
  }

  private static Stream<Arguments> responseProcessingFailureCases() {
    return Stream.of( // logLevel, request, response, responseBody, error
        Arguments.of(ActionLogLevel.INFO, true, true, false, true),
        Arguments.of(ActionLogLevel.ERROR, true, true, false, true)
    );
  }

  private static Stream<Arguments> unexpectedErrorAfterProcessingCases() {
    return Stream.of( // logLevel, request, response, responseBody, error
        Arguments.of(ActionLogLevel.INFO, true, true, true, true),
        Arguments.of(ActionLogLevel.ERROR, true, true, false, true)
    );
  }

  @BeforeEach
  void setUp() {
    endpointOptions = new EndpointOptions()
        .setDomain("http://api.service.com")
        .setPort(8080);
    endpointRequest = new EndpointRequest("/api/v1/data.json", MultiMap.caseInsensitiveMultiMap());
  }

  @ParameterizedTest
  @MethodSource("happyPathCases")
  @DisplayName("Expect log to contain desired information when request creation, response and responseBody are logged")
  void testRequestSucceeded(ActionLogLevel level, boolean expectRequest, boolean expectResponse,
      boolean expectResponseBody, boolean expectError) {
    givenLogger(level);
    givenResponse(HttpStatus.SC_OK);

    whenRequestSucceeded();

    thenLogContainsRequestData(expectRequest);
    thenLogContainsResponse(expectResponse);
    thenLogContainsResponseBody(expectResponseBody);
    thenLogContainsError(expectError);
  }

  private void whenRequestSucceeded() {
    tested.onRequestCreation(endpointRequest);
    tested.onRequestSucceeded(httpResponse);
    tested.onResponseCodeSuccessful();
  }

  @ParameterizedTest
  @MethodSource("requestFailureCases")
  @DisplayName("Expect log to contain desired information when request creation and request failure are logged")
  void testRequestFailure(ActionLogLevel level, boolean expectRequest, boolean expectResponse,
      boolean expectResponseBody, boolean expectError) {
    givenLogger(level);

    whenRequestFailed();

    thenLogContainsRequestData(expectRequest);
    thenLogContainsResponse(expectResponse);
    thenLogContainsResponseBody(expectResponseBody);
    thenLogContainsError(expectError);
  }

  @ParameterizedTest
  @MethodSource("responseCodeTimeoutCases")
  @DisplayName("Expect log to contain desired information when request creation, request failure and response unsuccessful code are logged")
  void testResponseCodeTimeout(ActionLogLevel level, boolean expectRequest, boolean expectResponse,
      boolean expectResponseBody, boolean expectError) {
    givenLogger(level);

    whenResponseCodeTimeout();

    thenLogContainsRequestData(expectRequest);
    thenLogContainsResponse(expectResponse);
    thenLogContainsResponseBody(expectResponseBody);
    thenLogContainsError(expectError);
  }

  @ParameterizedTest
  @MethodSource("responseCodeUnsuccessfulCases")
  @DisplayName("Expect log to contain desired information when request creation and response unsuccessful code are logged")
  void testResponseCodeUnsuccessful(ActionLogLevel level, boolean expectRequest, boolean expectResponse,
      boolean expectResponseBody, boolean expectError) {
    givenLogger(level);
    givenResponse(HttpStatus.SC_NOT_FOUND);

    whenResponseCodeUnsuccessful();

    thenLogContainsRequestData(expectRequest);
    thenLogContainsResponse(expectResponse);
    thenLogContainsResponseBody(expectResponseBody);
    thenLogContainsError(expectError);
  }

  @ParameterizedTest
  @MethodSource("responseProcessingFailureCases")
  @DisplayName("Expect log to contain desired information when request creation, response and response processing failure are logged")
  void testResponseProcessingFailure(ActionLogLevel level, boolean expectRequest, boolean expectResponse,
      boolean expectResponseBody, boolean expectError) {
    givenLogger(level);
    givenResponse(HttpStatus.SC_OK);

    whenResponseProcessingFailed();

    thenLogContainsRequestData(expectRequest);
    thenLogContainsResponse(expectResponse);
    thenLogContainsResponseBody(expectResponseBody);
    thenLogContainsError(expectError);
  }

  @ParameterizedTest
  @MethodSource("unexpectedErrorAfterProcessingCases")
  @DisplayName("Expect log to contain desired information when request creation, response, responseBody and unexpected error are logged")
  void testUnexpectedErrorAfterResponseProcessing(ActionLogLevel level, boolean expectRequest, boolean expectResponse,
      boolean expectResponseBody, boolean expectError) {
    givenLogger(level);
    givenResponse(HttpStatus.SC_OK);

    whenUnexpectedErrorAfterResponseProcessing();

    thenLogContainsRequestData(expectRequest);
    thenLogContainsResponse(expectResponse);
    thenLogContainsResponseBody(expectResponseBody);
    thenLogContainsError(expectError);
  }

  private void givenLogger(ActionLogLevel level) {
    tested = HttpActionLogger.create(SAMPLE_ACTION_ALIAS, level, endpointOptions, HttpMethod.GET.name());
  }

  private void givenResponse(int httpStatus) {
    when(httpResponse.version()).thenReturn(HttpVersion.HTTP_2);
    when(httpResponse.statusCode()).thenReturn(httpStatus);
    when(httpResponse.statusMessage()).thenReturn("");
    when(httpResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(httpResponse.trailers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
  }

  private void whenRequestFailed() {
    tested.onRequestCreation(endpointRequest);
    tested.onRequestFailed(new IOException("Network error"));
  }

  private void whenResponseCodeTimeout() {
    tested.onRequestCreation(endpointRequest);
    tested.onRequestFailed(new TimeoutException());
    tested.onResponseCodeUnsuccessful(new IOException("Unsuccessful response code"));
  }

  private void whenResponseCodeUnsuccessful() {
    tested.onRequestCreation(endpointRequest);
    tested.onRequestSucceeded(httpResponse);
    tested.onResponseCodeUnsuccessful(new IOException("Unsuccessful response code"));
  }

  private void whenResponseProcessingFailed() {
    tested.onRequestCreation(endpointRequest);
    tested.onRequestSucceeded(httpResponse);
    tested.onResponseProcessingFailed(new IOException("Invalid response code"));
  }

  private void whenUnexpectedErrorAfterResponseProcessing() {
    tested.onRequestCreation(endpointRequest);
    tested.onRequestSucceeded(httpResponse);
    tested.onResponseCodeSuccessful();
    tested.onDifferentError(new NullPointerException("Internal reference null"));
  }

  private void thenLogContainsRequestData(boolean positive) {
    JsonObject logs = tested.getJsonNodeLog().getJsonObject("logs");
    if (positive) {
      assertNotNull(logs.getJsonObject(REQUEST));
      assertRequestLogs(logs.getJsonObject(REQUEST));
    } else {
      assertNull(logs.getJsonObject(REQUEST));
    }
  }

  private void thenLogContainsResponse(boolean positive) {
    JsonObject logs = tested.getJsonNodeLog().getJsonObject("logs");
    if (positive) {
      assertNotNull(logs.getJsonObject(RESPONSE));
      assertResponseLogs(logs.getJsonObject(RESPONSE));
    } else {
      assertNull(logs.getJsonObject(RESPONSE));
    }
  }

  private void thenLogContainsResponseBody(boolean positive) {
    JsonObject logs = tested.getJsonNodeLog().getJsonObject("logs");
    if (positive) {
      assertNotNull(logs.getString(RESPONSE_BODY));
    } else {
      assertNull(logs.getString(RESPONSE_BODY));
    }
  }

  private void thenLogContainsError(boolean positive) {
    JsonObject logs = tested.getJsonNodeLog().getJsonObject("logs");
    if (positive) {
      assertNotNull(logs.getJsonArray(ERRORS));
      assertErrorLogs(logs.getJsonArray(ERRORS));
    } else {
      assertNull(logs.getJsonArray(ERRORS));
    }
  }

  private static void assertRequestLogs(JsonObject requestLog) {
    assertTrue(requestLog.containsKey("path"));
    assertTrue(requestLog.containsKey("requestHeaders"));
  }

  private static void assertErrorLogs(JsonArray errorLog) {
    errorLog.forEach(e -> {
      ((JsonObject) e).containsKey("className");
      ((JsonObject) e).containsKey("message");
    });
  }

  private static void assertResponseLogs(JsonObject responseLog) {
    assertTrue(responseLog.containsKey("httpVersion"));
    assertTrue(responseLog.containsKey("httpMethod"));
    assertTrue(responseLog.containsKey("statusCode"));
    assertTrue(responseLog.containsKey("statusMessage"));
    assertTrue(responseLog.containsKey("headers"));
    assertTrue(responseLog.containsKey("trailers"));
    assertTrue(responseLog.containsKey("requestPath"));
  }

}
