package io.knotx.fragments.action.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.knotx.fragments.action.http.options.EndpointOptions;
import io.knotx.fragments.action.http.options.HttpActionOptions;
import io.knotx.fragments.action.http.request.EndpointRequest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.concurrent.TimeUnit;
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

  @BeforeEach
  void setUp() {
    server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
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
  void shouldNotEncodePath(String path, Vertx vertx, VertxTestContext testContext)
      throws InterruptedException {
    // given
    EndpointRequest endpointRequest = new EndpointRequest(path, MultiMap.caseInsensitiveMultiMap());
    configureServer(path);
    WebClient webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx));

    EndpointInvoker tested = new EndpointInvoker(webClient, createOptions());

    // when, then
    tested.invokeEndpoint(endpointRequest).subscribe(
        response -> {
          testContext.verify(() -> assertEquals(400, response.statusCode()));
          testContext.completeNow();
        },
        testContext::failNow
    );
    assertTrue(testContext.awaitCompletion(60, TimeUnit.SECONDS));
  }

  @ParameterizedTest
  @MethodSource("headersWithSpecialCharsInValues")
  @DisplayName("Expect 200 response for headers values that are not encoded by EndpointInvoker")
  void shouldAcceptNotEncodedHeaderValues(String headerName, String headerValue, Vertx vertx,
      VertxTestContext testContext)
      throws InterruptedException {
    // given
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(headerName, headerValue);

    EndpointRequest endpointRequest = new EndpointRequest("/", headers);
    WebClient webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx));
    configureServer(headerName, headerValue);

    EndpointInvoker tested = new EndpointInvoker(webClient, createOptions());

    // when, then
    tested.invokeEndpoint(endpointRequest).subscribe(
        response -> {
          testContext.verify(() -> assertEquals(200, response.statusCode()));
          testContext.completeNow();
        },
        testContext::failNow
    );
    assertTrue(testContext.awaitCompletion(60, TimeUnit.SECONDS));
  }

  @ParameterizedTest
  @MethodSource("headersWithIllegalCharsInNames")
  @DisplayName("Expect exception raised by WebClient when header name is illegal")
  void shouldNotAllowIllegalHeaders(String headerName, String headerValue, Vertx vertx,
      VertxTestContext testContext) throws InterruptedException {
    // given
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(headerName, headerValue);

    EndpointRequest endpointRequest = new EndpointRequest("/", headers);
    WebClient webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx));

    EndpointInvoker tested = new EndpointInvoker(webClient, createOptions());

    // when, then
    tested.invokeEndpoint(endpointRequest).subscribe(
        response -> testContext.failNow(new IllegalStateException(String
            .format("Exception not thrown for parameters [%s] [%s]", headerName, headerValue))),
        error -> {
          assertEquals(IllegalArgumentException.class, error.getClass());
          testContext.completeNow();
        }
    );
    assertTrue(testContext.awaitCompletion(60, TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("Expect WebClient to ignore header with empty name, but WireMockServer expecting it")
  void shouldIgnoreEmptyHeaderButServerExpects(Vertx vertx, VertxTestContext testContext)
      throws InterruptedException {
    // given
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(StringUtils.EMPTY, "some-value");
    configureServer(StringUtils.EMPTY, "some-value");

    EndpointRequest endpointRequest = new EndpointRequest("/", headers);
    WebClient webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx));

    EndpointInvoker tested = new EndpointInvoker(webClient, createOptions());

    // when, then
    tested.invokeEndpoint(endpointRequest).subscribe(
        response -> {
          testContext.verify(() -> assertEquals(404, response.statusCode()));
          testContext.completeNow();
        },
        testContext::failNow
    );
    assertTrue(testContext.awaitCompletion(60, TimeUnit.SECONDS));
  }

  private void configureServer(String path) {
    server.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(200)));
  }

  private void configureServer(String headerName, String headerValue) {
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

}
