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
package io.knotx.fragments.handler.consumer.html;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumerFactory;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FragmentHtmlBodyWriterFactory implements FragmentExecutionLogConsumerFactory {

  static final String FRAGMENT_TYPES_OPTIONS = "fragmentTypes";
  static final String CONDITION_OPTION = "condition";
  static final String HEADER_OPTION = "header";

  private static final String PARAM_OPTION = "param";

  @Override
  public String getName() {
    return "fragmentHtmlBodyWriter";
  }

  @Override
  public FragmentExecutionLogConsumer create(JsonObject config) {
    return new FragmentExecutionLogConsumer() {

      private Set<String> supportedTypes = getSupportedTypes(config);
      private String requestHeader = getConditionHeader(config);
      private String requestParam = getConditionParam(config);

      @Override
      public void accept(ClientRequest request, List<FragmentExecutionLog> executions) {
        if (containsHeader(request) || containsParam(request)) {
          executions.stream()
              .filter(this::isSupported)
              .forEach(this::wrapFragmentBodyWithMetadata);
        }
      }

      private void wrapFragmentBodyWithMetadata(FragmentExecutionLog executionData) {
        wrapFragmentBody(executionData.getFragment(), executionData.toJson());
      }

      private void wrapFragmentBody(Fragment fragment, JsonObject log) {
        fragment.setBody("<!-- data-knotx-id=\"" + fragment.getId() + "\" -->"
            + logAsScript(fragment.getId(), log)
            + fragment.getBody()
            + "<!-- data-knotx-id=\"" + fragment.getId() + "\" -->");
      }

      private String logAsScript(String fragmentId, JsonObject log) {
        return "<script data-knotx-debug=\"log\" data-knotx-id=\"" + fragmentId
            + "\" type=\"application/json\">"
            + log +
            "</script>";
      }

      private boolean containsHeader(ClientRequest request) {
        return Optional.ofNullable(requestHeader)
            .map(header -> request.getHeaders().contains(header))
            .orElse(Boolean.FALSE);
      }

      private boolean containsParam(ClientRequest request) {
        return Optional.ofNullable(requestParam)
            .map(param -> request.getParams().contains(param))
            .orElse(Boolean.FALSE);
      }

      private boolean isSupported(FragmentExecutionLog executionData) {
        return supportedTypes.contains(executionData.getFragment().getType());
      }
    };
  }

  private Set<String> getSupportedTypes(JsonObject config) {
    if (config.containsKey(FRAGMENT_TYPES_OPTIONS)) {
      JsonArray fragmentTypes = config.getJsonArray(FRAGMENT_TYPES_OPTIONS);
      return StreamSupport.stream(fragmentTypes.spliterator(), false)
          .map(Object::toString)
          .collect(Collectors.toSet());
    } else {
      return Collections.emptySet();
    }
  }

  private String getConditionHeader(JsonObject config) {
    return config.getJsonObject(CONDITION_OPTION, new JsonObject()).getString(HEADER_OPTION);
  }

  private String getConditionParam(JsonObject config) {
    return config.getJsonObject(CONDITION_OPTION, new JsonObject()).getString(PARAM_OPTION);
  }
}
