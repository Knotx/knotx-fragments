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
package io.knotx.fragments.action.library.http.request.placeholders;

import static io.knotx.fragments.action.library.helper.FragmentPlaceholders.buildSourceDefinitions;

import io.knotx.fragments.action.library.http.options.EndpointOptions;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.server.common.placeholders.JsonResolver;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.PlaceholdersResolver.Builder;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.vertx.core.json.JsonObject;

public class EndpointPlaceholdersResolver {

  private final PlaceholdersResolver pathResolver;
  private final PlaceholdersResolver bodyResolver;
  private final JsonResolver jsonResolver;

  public EndpointPlaceholdersResolver(EndpointOptions options, FragmentContext fragmentContext) {
    SourceDefinitions sources = buildSourceDefinitions(fragmentContext);
    pathResolver = buildResolver(sources, options.isClearUnmatchedPlaceholdersInPath(),
        options.isEncodePlaceholdersInPath());
    bodyResolver = buildResolver(sources, options.isClearUnmatchedPlaceholdersInBodyString(),
        options.isEncodePlaceholdersInBodyString());
    jsonResolver = new JsonResolver(
        buildResolver(sources, options.isClearUnmatchedPlaceholdersInBodyJson(),
            options.isEncodePlaceholdersInBodyJson()));
  }

  public String resolvePath(String input) {
    return pathResolver.resolve(input);
  }

  public String resolveBody(String input) {
    return bodyResolver.resolve(input);
  }

  public JsonObject resolveJson(JsonObject input) {
    return jsonResolver.resolveJson(input);
  }

  static PlaceholdersResolver buildResolver(SourceDefinitions sources, boolean clearUnmatched,
      boolean encode) {
    Builder builder = PlaceholdersResolver.builder().withSources(sources);
    if (!clearUnmatched) {
      builder.leaveUnmatched();
    }
    if (encode) {
      builder.encodeValues();
    }
    return builder.build();
  }

}
