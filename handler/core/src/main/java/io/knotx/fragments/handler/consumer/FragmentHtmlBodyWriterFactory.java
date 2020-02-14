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
package io.knotx.fragments.handler.consumer;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventWithTaskMetadata;
import io.knotx.fragments.engine.TaskMetadata;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FragmentHtmlBodyWriterFactory implements FragmentEventsConsumerFactory {

  static final String FRAGMENT_TYPES_OPTIONS = "fragmentTypes";
  static final String CONDITION_OPTION = "condition";
  static final String HEADER_OPTION = "header";

  private static final String PARAM_OPTION = "param";

  @Override
  public String getName() {
    return "fragmentHtmlBodyWriter";
  }

  @Override
  public FragmentEventsConsumer create(JsonObject config) {
    return new FragmentEventsConsumer() {
      private Set<String> supportedTypes = getSupportedTypes(config);
      private String requestHeader = getConditionHeader(config);
      private String requestParam = getConditionParam(config);

      @Override
      public void accept(ClientRequest request, List<FragmentEventWithTaskMetadata> events) {
        if (containsHeader(request) || containsParam(request)) {
          events.stream()
              .filter(this::isSupported)
              .forEach(this::wrapFragmentBody);
        }
      }

      private void wrapFragmentBody(FragmentEventWithTaskMetadata fragmentEventWithTaskMetadata) {
        TaskMetadata taskMetadata = fragmentEventWithTaskMetadata.getTaskMetadata();
        FragmentEvent fragmentEvent = fragmentEventWithTaskMetadata.getFragmentEvent();
        Fragment fragment = fragmentEvent.getFragment();
        fragment.setBody("<!-- data-knotx-id=\"" + fragment.getId() + "\" -->"
            + addNodeLogAsScript(fragmentEvent)
            + addGraphAsScript(fragmentEvent.getFragment().getId(), taskMetadata)
            + fragment.getBody()
            + "<!-- data-knotx-id=\"" + fragment.getId() + "\" -->");
      }

      private String addNodeLogAsScript(FragmentEvent event) {
        return "<script data-knotx-debug=\"log\" data-knotx-id=\"" + event.getFragment().getId()
            + "\" type=\"application/json\">"
            + event.toJson() +
            "</script>";
      }

      private String addGraphAsScript(String fragmentId, TaskMetadata taskMetadata) {
        return "<script data-knotx-debug=\"graph\" data-knotx-id=\"" + fragmentId
            + "\" type=\"application/json\">"
            + taskMetadata.getMetadata() +
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

      private boolean isSupported(FragmentEventWithTaskMetadata fragmentEventWithTaskMetadata) {
        return supportedTypes
            .contains(fragmentEventWithTaskMetadata.getFragmentEvent().getFragment().getType());
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
