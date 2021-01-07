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
package io.knotx.fragments.action.library.http.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.fragments.action.library.http.options.EndpointOptions;
import io.knotx.fragments.action.library.http.request.placeholders.EndpointPlaceholdersResolver;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EndpointPlaceholdersResolverTest {

  private static final String HOST_HEADER = "domain.com";
  private static final String REFINED_QUERY_PARAM = "someComplexValue";
  private static final String HTTP_REQUEST_SOURCE = "/api/v1/stuff";
  private static final String API_KEY = "SEousaebES73task4!@%Tyiq3tgs08#^w#JSNB";
  private static final String ENCODED_API_KEY = "SEousaebES73task4%21%40%25Tyiq3tgs08%23%5Ew%23JSNB";

  private static final String STRING_NO_PLACEHOLDERS = "<a href=\"http://www.some.page.com/path/page.html\">Some text with no placeholders</a>";
  private static final String PAYLOAD_API_KEY_PLACEHOLDER = "{payload.api-key}";
  private static final String STRING_WITH_PLACEHOLDERS =
      "<a href=\"{header.host}\">{header.content-type}</a>"
          + "<a href=\"{param.refinedQuery}\">{param.code}</a>"
          + "<a href=\"{payload.httpCall._request.source}\">{payload.httpCall._result}</a>";
  private static final String STRING_EMPTIED_PLACEHOLDERS = "<a href=\"\"></a><a href=\"\"></a><a href=\"\"></a>";
  private static final String STRING_SOME_PLACEHOLDERS_FILLED_OTHER_EMPTIED =
      "<a href=\"" + HOST_HEADER + "\"></a><a href=\"" + REFINED_QUERY_PARAM + "\"></a><a href=\""
          + HTTP_REQUEST_SOURCE + "\"></a>";

  private static final String COMPLEX_JSON_STRING_WITH_PLACEHOLDERS = "{\"query\":\"query ($categories: [String!]!) {\\n    products(filter: { category_id: { in: $categories } }) {    \\n        total_count\\n        items {      \\n            name      \\n            description {\\n                html      \\n            }      \\n            image {\\n                url\\n                label\\n            }\\n            small_image {         \\n                url\\n                label\\n            }\\n        }\\n    }\\n}\",\"variables\":{\"categories\":[\"{config.categories}\"]}}";
  private static final String COMPLEX_JSON_STRING_PLACEHOLDERS_DONE = "{\"query\":\"query ($categories: [String!]!) {\\n    products(filter: { category_id: { in: $categories } }) {    \\n        total_count\\n        items {      \\n            name      \\n            description {\\n                html      \\n            }      \\n            image {\\n                url\\n                label\\n            }\\n            small_image {         \\n                url\\n                label\\n            }\\n        }\\n    }\\n}\",\"variables\":{\"categories\":[\"2\"]}}";

  private static final JsonObject JSON_NO_PLACEHOLDERS = new JsonObject()
      .put("api-key", "^mJHG3%#r6@")
      .put("items", new JsonObject()
          .put("itemCount", 2)
          .put("items", new JsonArray()
              .add(new JsonObject().put("product1", "code-UYO"))
              .add(new JsonObject().put("product2", "code-HYW"))));
  private static final JsonObject JSON_SINGLE_PLACEHOLDER = new JsonObject()
      .put("apiResponse", new JsonObject().put("apiKey", PAYLOAD_API_KEY_PLACEHOLDER));
  private static final JsonObject JSON_SINGLE_PLACEHOLDER_FILLED = new JsonObject()
      .put("apiResponse", new JsonObject().put("apiKey", API_KEY));
  private static final JsonObject JSON_WITH_PLACEHOLDERS = new JsonObject()
      .put("api-key", "{header.host}")
      .put("items", new JsonObject()
          .put("itemCount", 2)
          .put("{param.refinedQuery}", new JsonArray()
              .add(new JsonObject().put("product1", "{payload.httpCall._request.source}"))
              .add(new JsonObject().put("product2", "code-HYW"))));
  private static final JsonObject JSON_WITH_FILLED_PLACEHOLDERS = new JsonObject()
      .put("api-key", HOST_HEADER)
      .put("items", new JsonObject()
          .put("itemCount", 2)
          .put(REFINED_QUERY_PARAM, new JsonArray()
              .add(new JsonObject().put("product1", HTTP_REQUEST_SOURCE))
              .add(new JsonObject().put("product2", "code-HYW"))));
  private static final JsonObject JSON_NONEXISTENT_KEY_PLACEHOLDERS = new JsonObject()
      .put("api-key", "^mJHG3%#r6@")
      .put("nested", new JsonObject()
          .put("{non.existent.placeholder}", "someValue")
          .put("{another.non.existent.placeholder}", "someOtherValue"));
  private static final JsonObject JSON_NONEXISTENT_VALUE_PLACEHOLDERS = new JsonObject()
      .put("api-key", "^mJHG3%#r6@")
      .put("nested", new JsonObject()
          .put("ElementA", "{non.existent.placeholder}")
          .put("ElementB", "{another.non.existent.placeholder}"));
  private static final JsonObject JSON_NONEXISTENT_VALUE_PLACEHOLDERS_FILLED = new JsonObject()
      .put("api-key", "^mJHG3%#r6@")
      .put("nested", new JsonObject()
          .put("ElementA", "")
          .put("ElementB", ""));

  private EndpointPlaceholdersResolver tested;

  @Test
  @DisplayName("Expect unchanged plaintext when no placeholders present and none provided")
  void plainTextNoPlaceholders() {
    givenDefaultResolverFor(emptyFragmentContext());

    String result = tested.resolvePath(STRING_NO_PLACEHOLDERS);

    assertEquals(STRING_NO_PLACEHOLDERS, result);
  }

  @Test
  @DisplayName("Expect empty strings for placeholders not present in FragmentContext")
  void placeholdersNotProvided() {
    givenDefaultResolverFor(emptyFragmentContext());

    String result = tested.resolvePath(STRING_WITH_PLACEHOLDERS);

    assertEquals(STRING_EMPTIED_PLACEHOLDERS, result);
  }

  @Test
  @DisplayName("Expect provided placeholders to be filled")
  void providedPlaceholdersFilled() {
    givenDefaultResolverFor(fragmentContextWithSomeData());

    String result = tested.resolvePath(STRING_WITH_PLACEHOLDERS);

    assertEquals(STRING_SOME_PLACEHOLDERS_FILLED_OTHER_EMPTIED, result);
  }

  @Test
  @DisplayName("Expect unescaped value to be interpolated from Fragment's payload")
  void singlePlaceholderForPayload() {
    givenDefaultResolverFor(fragmentContextWithApiKey());

    String result = tested.resolveBody(PAYLOAD_API_KEY_PLACEHOLDER);

    assertEquals(API_KEY, result);
  }

  @Test
  @DisplayName("Expect value to be interpolated and escaped from Fragment's payload")
  void singleEscapedPlaceholderForPayload() {
    givenDefaultResolverFor(fragmentContextWithApiKey());

    String result = tested.resolvePath(PAYLOAD_API_KEY_PLACEHOLDER);

    assertEquals(ENCODED_API_KEY, result);
  }

  @Test
  @DisplayName("Expect empty JSON to be passed as-is")
  void jsonEmptyLeftAsIs() {
    givenDefaultResolverFor(emptyFragmentContext());

    JsonObject result = tested.resolveJson(new JsonObject());

    assertEquals(new JsonObject(), result);
  }

  @Test
  @DisplayName("Expect JSON without placeholders to be passed as-is")
  void jsonWithoutPlaceholdersLeftAsIs() {
    givenDefaultResolverFor(emptyFragmentContext());

    JsonObject result = tested.resolveJson(JSON_NO_PLACEHOLDERS);

    assertEquals(JSON_NO_PLACEHOLDERS, result);
  }

  @Test
  @DisplayName("Expect unescaped value in JSON to be interpolated from Fragment's payload")
  void jsonSinglePlaceholderForPayload() {
    givenDefaultResolverFor(fragmentContextWithApiKey());

    JsonObject result = tested.resolveJson(JSON_SINGLE_PLACEHOLDER);

    assertEquals(JSON_SINGLE_PLACEHOLDER_FILLED, result);
  }

  @Test
  @DisplayName("Expect placeholders in JSON to be filled")
  void jsonWithPlaceholdersFilled() {
    givenDefaultResolverFor(fragmentContextWithSomeData());

    JsonObject result = tested.resolveJson(JSON_WITH_PLACEHOLDERS);

    assertEquals(JSON_WITH_FILLED_PLACEHOLDERS, result);
  }

  @Test
  @DisplayName("Expect non existent placeholders in JSON values to be replaced with empty string")
  void jsonWithNonExistentPlaceholdersInValuesReplacedWithEmptyString() {
    givenDefaultResolverFor(fragmentContextWithSomeData());

    JsonObject result = tested.resolveJson(JSON_NONEXISTENT_VALUE_PLACEHOLDERS);

    assertEquals(JSON_NONEXISTENT_VALUE_PLACEHOLDERS_FILLED, result);
  }

  @Test
  @DisplayName("Expect interpolating a JSON key to an empty string results in an exception")
  void jsonEmptyKeyInterpolationFails() {
    givenClearingUnmatchedResolverFor(emptyFragmentContext());

    assertThrows(IllegalStateException.class, () -> tested.resolveJson(
        JSON_NONEXISTENT_KEY_PLACEHOLDERS));
  }

  @Test
  @DisplayName("Expect parsed complex JSON string with nested placeholders to be replaced")
  void complexJsonAsStringIsInterpolated() {
    givenDefaultResolverFor(fragmentContextWithSomeData());

    String result = tested.resolveBody(COMPLEX_JSON_STRING_WITH_PLACEHOLDERS);

    assertEquals(COMPLEX_JSON_STRING_PLACEHOLDERS_DONE, result);
  }

  private void givenDefaultResolverFor(FragmentContext fragmentContext) {
    tested = new EndpointPlaceholdersResolver(new EndpointOptions(), fragmentContext);
  }

  private void givenClearingUnmatchedResolverFor(FragmentContext fragmentContext) {
    tested = new EndpointPlaceholdersResolver(new EndpointOptions()
        .setClearUnmatchedPlaceholdersInPath(true)
        .setClearUnmatchedPlaceholdersInBodyString(true)
        .setClearUnmatchedPlaceholdersInBodyJson(true), fragmentContext);
  }

  private FragmentContext emptyFragmentContext() {
    return new FragmentContext(
        new Fragment("snippet", new JsonObject(), StringUtils.EMPTY),
        new ClientRequest()
    );
  }

  private FragmentContext fragmentContextWithSomeData() {
    return new FragmentContext(
        new Fragment("snippet", new JsonObject().put("categories", 2), StringUtils.EMPTY)
            .appendPayload("httpCall", new JsonObject()
                .put("_request", new JsonObject().put("source", HTTP_REQUEST_SOURCE))),
        new ClientRequest().setHeaders(MultiMap.caseInsensitiveMultiMap()
            .add("Host", HOST_HEADER))
            .setParams(MultiMap.caseInsensitiveMultiMap().add("refinedQuery", REFINED_QUERY_PARAM))
    );
  }

  private FragmentContext fragmentContextWithApiKey() {
    return new FragmentContext(
        new Fragment("snippet", new JsonObject(), StringUtils.EMPTY)
            .appendPayload("api-key", API_KEY),
        new ClientRequest()
    );
  }

}
