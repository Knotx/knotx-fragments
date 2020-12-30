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

import static io.knotx.fragments.action.library.helper.FragmentPlaceholders.buildSourceDefinitions;

import io.knotx.fragments.api.FragmentContext;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

class EndpointPlaceholdersResolver {

  private final PlaceholdersResolver simpleResolverLeavingUnmatched;
  private final PlaceholdersResolver encodingResolver;

  EndpointPlaceholdersResolver(FragmentContext fragmentContext) {
    SourceDefinitions sourceDefinitions = buildSourceDefinitions(fragmentContext);
    simpleResolverLeavingUnmatched = PlaceholdersResolver.builder()
        .withSources(sourceDefinitions)
        .leaveUnmatched()
        .build();
    encodingResolver = PlaceholdersResolver.createEncoding(sourceDefinitions);
  }

  String resolve(String input) {
    return simpleResolverLeavingUnmatched.resolve(input);
  }

  JsonObject resolve(JsonObject input) {
    JsonObject output = new JsonObject();
    input.forEach(
        entry -> output.put(resolveNotEmpty(entry.getKey()), resolveInternal(entry.getValue())));
    return output;
  }

  String resolveAndEncode(String input) {
    return encodingResolver.resolve(input);
  }

  private String resolveNotEmpty(String input) {
    return Optional.of(input)
        .map(this::resolve)
        .filter(StringUtils::isNotEmpty)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Resolving [%s] resulted in a forbidden empty string", input)));
  }

  private Object resolveInternal(Object object) {
    if (object instanceof JsonObject) {
      return resolve((JsonObject) object);
    } else if (object instanceof JsonArray) {
      JsonArray array = (JsonArray) object;
      List<Object> list = array.stream().map(this::resolveInternal).collect(Collectors.toList());
      return new JsonArray(list);
    } else if (object instanceof String) {
      return simpleResolverLeavingUnmatched.resolve((String) object);
    } else {
      return object;
    }
  }

}
