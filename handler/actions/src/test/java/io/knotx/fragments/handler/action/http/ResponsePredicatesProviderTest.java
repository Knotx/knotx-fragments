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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.fragments.handler.action.http.request.ResponsePredicatesProvider;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResponsePredicatesProviderTest {

  private static final String STATUS_200 = "SC_OK";

  private static final String STATUS_404 = "SC_NOT_FOUND";

  private static final String JSON = "JSON";

  private static final String NON_EXISTING = "NON_EXISTING";

  private static final String SC_OK_LOWER_CASE = "sc_ok";

  private ResponsePredicatesProvider predicatesProvider = new ResponsePredicatesProvider();

  static Stream<Arguments> dataResponsePredicates() {
    return Stream.of(
        Arguments.of(STATUS_200, ResponsePredicate.SC_OK),
        Arguments.of(STATUS_404, ResponsePredicate.SC_NOT_FOUND),
        Arguments.of(JSON, ResponsePredicate.JSON),
        Arguments.of(SC_OK_LOWER_CASE, ResponsePredicate.SC_OK)
    );
  }

  static Stream<Arguments> dataNonExistingPredicates() {
    return Stream.of(
        Arguments.of(NON_EXISTING)
    );
  }

  @ParameterizedTest(name = "Expect valid response predicate")
  @MethodSource("dataResponsePredicates")
  void shouldReturnValidResponsePredicate(String name, ResponsePredicate expectedPredicate) {
    assertEquals(expectedPredicate, predicatesProvider.fromName(name));
  }

  @ParameterizedTest(name = "Should throw exception when nonexisting predicate requested")
  @MethodSource("dataNonExistingPredicates")
  void shouldThrowExceptionWhenNonExistingPredicateRequested(String predicate) {
    assertThrows(IllegalArgumentException.class, () -> predicatesProvider.fromName(predicate));
  }
}
