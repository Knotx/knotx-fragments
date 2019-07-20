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
package io.knotx.fragments.handler.debug;

import java.util.List;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.vertx.core.json.JsonObject;

public enum FragmentsDebugModeDecorator {

  SNIPPET_TYPE("snippet", new HtmlDebugModeStrategy()),
  JSON_OBJECT_TYPE("json", new JsonObjectDebugModeStrategy()),
  DEFAULT(null, new DefaultDebugModeStrategy());

  private final FragmentsDebugModeStrategy strategy;
  private final String type;

  FragmentsDebugModeDecorator(String type, FragmentsDebugModeStrategy strategy) {
    this.strategy = strategy;
    this.type = type;
  }

  public static FragmentsDebugModeDecorator getFragmentsDebugModeDecorator(boolean debugMode,
      List<FragmentEventContextTaskAware> events) {
    if (!debugMode) {
      return DEFAULT;
    }

    return getFragmentsDebugModeDecorator(events);
  }

  public void markAsDebuggable(boolean debugMode, List<FragmentEventContextTaskAware> events) {
    if (debugMode) {
      events.stream().filter(this::hasTask).forEach(this::markAsDebuggable);
    }
  }

  public void addDebugAssetsAndData(boolean debugMode, List<FragmentEvent> fragmentEvents) {
    if (debugMode) {
      JsonObject debugData = new JsonObject();
      fragmentEvents.stream()
          .filter(this::isDebugged)
          .forEach(fragmentEvent -> {
            appendFragmentPayload(fragmentEvent);
            appendFragmentLog(fragmentEvent);
            Fragment fragment = fragmentEvent.getFragment();
            debugData.put(fragment.getId(), fragmentEvent.getDebugData());
          });

      strategy.updateBodyWithDebugData(debugData, fragmentEvents);
    }
  }

  private static FragmentsDebugModeDecorator getFragmentsDebugModeDecorator(
      List<FragmentEventContextTaskAware> events) {
    if (hasAnyFragmentType(events, SNIPPET_TYPE.getType())) {
      return SNIPPET_TYPE;
    }

    if (isJsonObject(events)) {
      return JSON_OBJECT_TYPE;
    }

    return DEFAULT;
  }

  private static boolean hasAnyFragmentType(List<FragmentEventContextTaskAware> events,
      String type) {
    return events.stream().anyMatch(event -> type
        .equals(event.getFragmentEventContext().getFragmentEvent().getFragment().getType()));
  }

  private static boolean isJsonObject(List<FragmentEventContextTaskAware> events) {
    return events.size() == 1 && hasAnyFragmentType(events, JSON_OBJECT_TYPE.getType());
  }

  private void appendFragmentPayload(FragmentEvent fragmentEvent) {
    fragmentEvent.getDebugData()
        .put("payload", fragmentEvent.getFragment()
            .getPayload());
  }

  private void appendFragmentLog(FragmentEvent fragmentEvent) {
    fragmentEvent.getDebugData()
        .put("logs", fragmentEvent.getLogAsJson());
  }

  private boolean isDebugged(FragmentEvent fragmentEvent) {
    return fragmentEvent.getDebugData().containsKey("debug");
  }

  private void markAsDebuggable(FragmentEventContextTaskAware fragmentEventContextTaskAware) {
    FragmentEvent fragmentEvent = fragmentEventContextTaskAware.getFragmentEventContext()
        .getFragmentEvent();
    fragmentEvent.getDebugData().put("debug", true);
    fragmentEvent.getDebugData().put("body", fragmentEvent.getFragment().getBody());
  }

  private boolean hasTask(FragmentEventContextTaskAware fragmentEventContextTaskAware) {
    return fragmentEventContextTaskAware.getTask().isPresent();
  }

  public String getType() {
    return type;
  }
}