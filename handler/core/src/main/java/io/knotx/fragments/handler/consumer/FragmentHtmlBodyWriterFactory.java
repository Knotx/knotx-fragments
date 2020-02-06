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
import io.knotx.fragments.task.TaskEventWrapper;
import io.knotx.fragments.task.TasksEventsWrapper;
import io.knotx.fragments.task.factory.node.NodeWithMetadata;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;

public class FragmentHtmlBodyWriterFactory implements FragmentEventsConsumerFactory {

  static final String FRAGMENT_TYPES_OPTIONS = "fragmentTypes";

  static final String CONDITION_OPTION = "condition";

  static final String HEADER_OPTION = "header";

  static final String PARAM_OPTION = "param";

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
      public void accept(ClientRequest request, TasksEventsWrapper tasksEvents) {
        if (containsHeader(request) || containsParam(request)) {
          tasksEvents.stream()
              .filter(taskEventWrapper -> isSupported(taskEventWrapper.getFragmentEvent()))
              .forEach(this::wrapFragmentBody);
        }
      }

      private void wrapFragmentBody(TaskEventWrapper taskEventWrapper) {
        Fragment fragment = taskEventWrapper.getFragmentEvent().getFragment();
        fragment.setBody("<!-- data-knotx-id=\"" + fragment.getId() + "\" -->"
            + addAsScript(taskEventWrapper)
            + fragment.getBody()
            + "<!-- data-knotx-id=\"" + fragment.getId() + "\" -->");
      }

      private String addAsScript(TaskEventWrapper taskEventWrapper) {
        return "<script data-knotx-debug=\"log\" data-knotx-id=\"" + taskEventWrapper.getFragmentEvent().getFragment().getId()
            + "\" type=\"application/json\">"
            + taskEventWrapper.getFragmentEvent().toJson()
            + "</script>"
            + taskEventWrapper.getTask().getRootNode()
            .map(NodeWithMetadata::getData)
            .map(JsonObject::toString)
            .map(graphData ->
                "<script data-knotx-debug=\"graph\" data-knotx-id=\"" + taskEventWrapper.getFragmentEvent().getFragment().getId()
                    + "\" type=\"application/json\">"
                    + graphData
                    + "</script>")
            .orElse(StringUtils.EMPTY);
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

      private boolean isSupported(FragmentEvent fragmentEvent) {
        return supportedTypes.contains(fragmentEvent.getFragment().getType());
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
