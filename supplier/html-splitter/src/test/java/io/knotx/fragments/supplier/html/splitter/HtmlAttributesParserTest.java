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
package io.knotx.fragments.supplier.html.splitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.junit5.util.FileReader;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmlAttributesParserTest {

  private static final String SOME_KEY = "some-parameter";

  private static final String SINGLE_QUOTES_ESCAPED = "\\' Some text \\'having\\' single quotes escaped \\'\\' and some \"double quotes\" \\'";
  private static final String DOUBLE_QUOTES_ESCAPED = "\\\" Some text \\\"having\\\" double quotes escaped '' and some 'single quotes' \\\"";

  private HtmlAttributesParser tested;

  private static String json;
  private static String base64;

  @BeforeAll
  static void setUpOnce() throws IOException {
    json = FileReader.readText("jsonDoubleQuotes.json");
    base64 = FileReader.readText("base64.txt");
  }

  @BeforeEach
  void setUp() {
    tested = new HtmlAttributesParser();
  }

  @Test
  void split_whenNull_expectNoAttributes() {
    // when
    List<Pair<String, String>> pairs = tested.get(null);

    // then
    assertTrue(pairs.isEmpty());
  }

  @Test
  void shouldHandleLargeString() {
    final int COUNT_PER_TYPE = 50;
    String tag = tagWithProblematicParameters(COUNT_PER_TYPE);

    List<Pair<String, String>> pairs = tested.get(tag);

    assertEquals(COUNT_PER_TYPE * 4, pairs.size());

    assertPresent(pairs, base64, 0, COUNT_PER_TYPE);
    assertPresent(pairs, SINGLE_QUOTES_ESCAPED, COUNT_PER_TYPE, COUNT_PER_TYPE);
    assertPresent(pairs, DOUBLE_QUOTES_ESCAPED, 2 * COUNT_PER_TYPE, COUNT_PER_TYPE);
    assertPresent(pairs, json, 3 * COUNT_PER_TYPE, COUNT_PER_TYPE);
  }

  private void assertPresent(List<Pair<String, String>> pairs, String value, int offset,
      int count) {
    for (int i = 0; i < count; i++) {
      assertEquals(SOME_KEY, pairs.get(offset + i).getKey());
      assertEquals(value, pairs.get(offset + i).getValue());
    }
  }

  @Test
  void split_whenEmptyString_expectNoAttributes() {
    // when
    List<Pair<String, String>> pairs = tested.get("");

    // then
    assertTrue(pairs.isEmpty());
  }

  @Test
  void split_whenOneAttributeDoubleMarks_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute=\"value\"");

    // then
    assertEquals(1, pairs.size());
    assertEquals("attribute", pairs.get(0).getKey());
    assertEquals("value", pairs.get(0).getValue());
  }

  @Test
  void split_whenOneAttributeSingleMarks_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute='value'");

    // then
    assertEquals(1, pairs.size());
    assertEquals("attribute", pairs.get(0).getKey());
    assertEquals("value", pairs.get(0).getValue());
  }

  @Test
  void split_whenOneEmptyAttributeDoubleMarks_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute=\"\"");

    // then
    assertEmptyAttribute(pairs);
  }

  @Test
  void split_whenOneEmptyAttributeSingleMarks_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute=''");

    // then
    assertEmptyAttribute(pairs);
  }

  @Test
  void split_whenOneAttributeWithDash_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("a-t-t-r-i-b-u-t-e=\"value\"");

    // then
    assertEquals(1, pairs.size());
    assertEquals("a-t-t-r-i-b-u-t-e", pairs.get(0).getKey());
    assertEquals("value", pairs.get(0).getValue());
  }

  @Test
  void split_whenOneAttributeWithWhitespaces_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute=\"value with space\"");

    // then
    assertEquals(1, pairs.size());
    assertEquals("attribute", pairs.get(0).getKey());
    assertEquals("value with space", pairs.get(0).getValue());
  }

  @Test
  void split_whenOneJsonAttributeDoubleMarks_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute=\"{\\\"key\\\"=\\\"value\\\"}\"");

    // then
    assertEquals(1, pairs.size());
    assertEquals("attribute", pairs.get(0).getKey());
    assertEquals("{\\\"key\\\"=\\\"value\\\"}", pairs.get(0).getValue());
  }

  @Test
  void split_whenOneJsonAttributeSingleMarks_expectOneAttribute() {
    // when
    List<Pair<String, String>> pairs = tested.get("attribute='{\"key\"=\"value\"}'");

    // then
    assertEquals(1, pairs.size());
    assertEquals("attribute", pairs.get(0).getKey());
    assertEquals("{\"key\"=\"value\"}", pairs.get(0).getValue());
  }

  @Test
  void split_whenManyAttribute_expectManyAttributes() {
    // when
    List<Pair<String, String>> pairs = tested.get(
        "attributeOne=\"{\\\"key\\\"=\\\"value\\\"}\" attributeTwo=\"valueTwo\" attributeThree=''");

    // then
    assertEquals(3, pairs.size());
    assertEquals("attributeOne", pairs.get(0).getKey());
    assertEquals("{\\\"key\\\"=\\\"value\\\"}", pairs.get(0).getValue());
    assertEquals("attributeTwo", pairs.get(1).getKey());
    assertEquals("valueTwo", pairs.get(1).getValue());
    assertEquals("attributeThree", pairs.get(2).getKey());
    assertEquals("", pairs.get(2).getValue());
  }

  private void assertEmptyAttribute(List<Pair<String, String>> pairs) {
    assertEquals(1, pairs.size());
    assertEquals("attribute", pairs.get(0).getKey());
    assertTrue(pairs.get(0).getValue().isEmpty());
  }

  private String tagWithProblematicParameters(int countPerType) {
    StringBuilder builder = new StringBuilder("<knotx:snippet");
    appendValue(builder, '\'', base64, countPerType);
    appendValue(builder, '\'', SINGLE_QUOTES_ESCAPED, countPerType);
    appendValue(builder, '"', DOUBLE_QUOTES_ESCAPED, countPerType);
    appendValue(builder, '\'', json, countPerType);
    builder.append(">");
    return builder.toString();
  }

  private static void appendValue(StringBuilder builder, char quote, String value, int count) {
    for (int i = 0; i < count; i++) {
      appendValue(builder, quote, value);
    }
  }

  private static void appendValue(StringBuilder builder, char quote, String value) {
    builder.append(" ");
    builder.append(SOME_KEY);
    builder.append("=");
    builder.append(quote);
    builder.append(value);
    builder.append(quote);
  }

}
