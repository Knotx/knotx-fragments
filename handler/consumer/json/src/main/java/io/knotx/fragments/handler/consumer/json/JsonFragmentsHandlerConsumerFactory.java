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
package io.knotx.fragments.handler.consumer.json;

import static java.lang.Boolean.FALSE;

import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumerFactory;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JsonFragmentsHandlerConsumerFactory implements FragmentExecutionLogConsumerFactory {

  private static final String PARAM_OPTION = "param";
  private static final String KNOTX_FRAGMENT = "_knotx_fragment";
  static final String FRAGMENT_TYPES_OPTIONS = "fragmentTypes";
  static final String HEADER_OPTION = "header";
  static final String CONDITION_OPTION = "condition";

  @Override
  public String getName() {
    return "fragmentJsonBodyWriter";
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
              .forEach(this::appendExecutionDataToFragmentBody);
        }
      }

      private boolean isSupported(FragmentExecutionLog executionData) {
        return supportedTypes.contains(executionData.getFragment().getType());
      }

      private boolean containsHeader(ClientRequest request) {
        return Optional.ofNullable(requestHeader)
            .map(header -> request.getHeaders().contains(header))
            .orElse(FALSE);
      }

      private boolean containsParam(ClientRequest request) {
        return Optional.ofNullable(requestParam)
            .map(param -> request.getParams().contains(param))
            .orElse(FALSE);
      }

      private void appendExecutionDataToFragmentBody(FragmentExecutionLog executionData) {
        JsonObject fragmentBody = new JsonObject().put(KNOTX_FRAGMENT, executionData.toJson())
            .mergeIn(new JsonObject(executionData.getFragment().getBody()));
        executionData.getFragment().setBody(fragmentBody.toString());
      }
    };
  }

  private Set<String> getSupportedTypes(JsonObject config) {
    return Optional.ofNullable(config.getJsonArray(FRAGMENT_TYPES_OPTIONS))
        .map(fragmentTypes -> StreamSupport.stream(fragmentTypes.spliterator(), false)
            .map(Object::toString)
            .collect(Collectors.toSet()))
        .orElse(Collections.emptySet());
  }

  private String getConditionHeader(JsonObject config) {
    return config.getJsonObject(CONDITION_OPTION, new JsonObject()).getString(HEADER_OPTION);
  }

  private String getConditionParam(JsonObject config) {
    return config.getJsonObject(CONDITION_OPTION, new JsonObject()).getString(PARAM_OPTION);
  }
}
