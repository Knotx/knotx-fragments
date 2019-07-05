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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

class HtmlAttributesParser {

  private static final String HTML_ATTRIBUTE_REGEXP = "(?<key>[\\w\\-]+)\\s*=\\s*(?<value>'((?:\\\\'|[^'])*)'|\"((?:\\\\\"|[^\"])*)\")";
  private static final Pattern ATTRIBUTE_PATTERN = Pattern
      .compile(HTML_ATTRIBUTE_REGEXP, Pattern.DOTALL);

  List<Pair<String, String>> get(String attributes) {
    List<Pair<String, String>> result = new ArrayList<>();
    if (StringUtils.isNotBlank(attributes)) {
      Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
      while (matcher.find()) {
        String key = matcher.group("key");
        String value = matcher.group("value");
        result.add(new ImmutablePair<>(key, value.substring(1, value.length() - 1)));
      }
    }
    return result;
  }

}
