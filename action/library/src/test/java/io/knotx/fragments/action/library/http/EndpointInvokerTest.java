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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.knotx.fragments.action.library.http.options.HttpActionOptions;
import io.knotx.fragments.action.library.http.request.EndpointRequest;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EndpointInvokerTest {

  private static final String SAMPLE_BODY = "{ \"token\": \"ousega@Q*#gsnls3\" }";

  @Mock
  private HttpRequest<Buffer> request;

  @Mock
  private HttpResponse<Buffer> response;

  @Mock
  private WebClient webClient;

  @ParameterizedTest
  @ValueSource(strings = {"GET", "HEAD", "DELETE"})
  @DisplayName("Expect body not send when body not supported by HTTP method ")
  void shouldNotSendBody(String httpMethod) {
    // given
    EndpointRequest endpointRequest = new EndpointRequest("/", MultiMap.caseInsensitiveMultiMap());
    HttpActionOptions options = sampleOptionsFor(httpMethod);
    mockHttpRequest(StringUtils.EMPTY);
    mockWebClient();

    EndpointInvoker tested = new EndpointInvoker(webClient, options);

    // when
    tested.invokeEndpoint(endpointRequest).subscribe();

    // then
    assertAll(
        () -> verify(request, times(1)).rxSend(),
        () -> verify(request, times(0)).rxSendBuffer(any())
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  @DisplayName("Expect body send when supported by HTTP method")
  void shouldSendBody(String httpMethod) {
    // given
    EndpointRequest endpointRequest = new EndpointRequest("/", MultiMap.caseInsensitiveMultiMap(),
        SAMPLE_BODY);
    HttpActionOptions options = sampleOptionsFor(httpMethod);

    mockHttpRequest(SAMPLE_BODY);
    mockWebClient();

    EndpointInvoker tested = new EndpointInvoker(webClient, options);

    // when
    tested.invokeEndpoint(endpointRequest).subscribe();

    // then
    assertAll(
        () -> verify(request, times(0)).rxSend(),
        () -> verify(request, times(1)).rxSendBuffer(Buffer.buffer(SAMPLE_BODY))
    );
  }

  private HttpActionOptions sampleOptionsFor(String httpMethod) {
    JsonObject configuration = new JsonObject()
        .put("httpMethod", httpMethod)
        .put("endpointOptions", new JsonObject()
            .put("domain", "https://api.service.com")
            .put("port", 8080));
    return new HttpActionOptions(configuration);
  }

  private void mockWebClient() {
    when(webClient.request(any(), anyInt(), any(), any())).thenReturn(request);
  }

  private void mockHttpRequest(String expectedBody) {
    when(request.timeout(anyLong())).thenReturn(request);
    when(request.putHeaders(any())).thenReturn(request);

    lenient().when(request.rxSend()).thenReturn(Single.just(response));
    lenient().when(request.rxSendBuffer(Buffer.buffer(expectedBody)))
        .thenReturn(Single.just(response));
  }

}
