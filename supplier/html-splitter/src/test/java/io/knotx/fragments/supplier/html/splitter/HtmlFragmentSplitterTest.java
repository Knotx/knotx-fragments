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

import io.knotx.fragment.Fragment;
import io.knotx.fragments.supplier.html.splitter.HtmlFragmentSplitter;
import io.knotx.junit5.util.FileReader;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmlFragmentSplitterTest {

  private HtmlFragmentSplitter tested;

  @BeforeEach
  void setUp() {
    tested = new HtmlFragmentSplitter();
  }

  @Test
  void split_whenNull_expectNoFragments() {
    // when
    List<Fragment> fragments = tested.split(null);

    // then
    assertTrue(fragments.isEmpty());
  }

  @Test
  void split_whenEmptyHtml_expectNoFragments() {
    // when
    List<Fragment> fragments = tested.split("");

    // then
    assertTrue(fragments.isEmpty());
  }

  @Test
  void split_whenOneStaticFragment_expectStaticFragment() throws IOException {
    // given
    String html = from("static-fragment.html");

    // when
    List<Fragment> fragments = tested.split(html);

    // then
    assertEquals(1, fragments.size());
    assertEquals("_STATIC", fragments.get(0).getType());
    assertEquals(html, fragments.get(0).getBody());
  }

  @Test
  void split_whenOneNonStaticFragment_expectNonStaticFragment() throws IOException {
    // given
    String html = from("dynamic-fragment.html");

    // when
    List<Fragment> fragments = tested.split(html);

    // then
    assertEquals(1, fragments.size());
    Fragment fragment = fragments.get(0);
    assertEquals("fragmentType", fragment.getType());
    assertEquals("valueOne", fragment.getConfiguration().getString("attributeOne"));
    assertEquals("valueTwo", fragment.getConfiguration().getString("attributeTwo"));
    assertEquals("", fragment.getConfiguration().getString("attributeEmpty"));
    assertEquals(from("dynamic-fragment-result.txt"), fragment.getBody());
  }

  @Test
  void split_whenManyFragments_expectManyFragments() throws IOException {
    // given
    String html = from("many-fragments.html");

    // when
    List<Fragment> actualFragments = tested.split(html);

    // then
    List<Fragment> expectedFragments = Arrays.asList(
        new Fragment("_STATIC", new JsonObject(), from("many-fragments-1.txt")),
        new Fragment("auth", new JsonObject().put("secret-key", "pass"),
            from("many-fragments-2.txt")),
        new Fragment("_STATIC", new JsonObject(), from("many-fragments-3.txt")),
        new Fragment("snippet", new JsonObject().put("knots", "any").put("any-key", "any-value"),
            from("many-fragments-4.txt")),
        new Fragment("snippet", new JsonObject().put("knots", "any-second"),
            from("many-fragments-5.txt")),
        new Fragment("_STATIC", new JsonObject(), from("many-fragments-6.txt")),
        new Fragment("fallback",
            new JsonObject().put("id", "1234").put("fallback-config", "{\"key\":\"value\"}"),
            from("many-fragments-7.txt")),
        new Fragment("_STATIC", new JsonObject(), from("many-fragments-8.txt"))
    );
    assertEquals(expectedFragments.size(), actualFragments.size());

    for (int i = 0; i < expectedFragments.size(); i++) {
      Fragment expected = expectedFragments.get(i);
      Fragment actual = actualFragments.get(i);
      assertEquals(expected.getType(), actual.getType());
      assertEquals(expected.getConfiguration(), actual.getConfiguration());
      assertEquals(expected.getBody().trim(), actual.getBody().trim());
    }
  }

  private String from(String fileName) throws IOException {
    return FileReader.readText(fileName);
  }

}
