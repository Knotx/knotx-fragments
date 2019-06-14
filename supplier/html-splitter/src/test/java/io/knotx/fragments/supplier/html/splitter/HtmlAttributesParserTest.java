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

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmlAttributesParserTest {

  private HtmlAttributesParser tested;

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

}
