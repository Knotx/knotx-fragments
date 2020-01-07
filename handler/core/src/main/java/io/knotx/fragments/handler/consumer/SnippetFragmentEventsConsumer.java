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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;

public class SnippetFragmentEventsConsumer implements FragmentEventsConsumer {
  private static final String BODY_SECTION_END = "</body>";

  private final String debugCss;
  private final String debugJs;

  SnippetFragmentEventsConsumer(String debugCss, String debugJs) {
    this.debugCss = debugCss;
    this.debugJs = debugJs;
  }

  @Override
  public void accept(List<FragmentEvent> fragmentEvents) {
    wrapWithFragmentId(fragmentEvents);
    JsonObject debugData = DebugDataRetriever.retrieveDebugData(fragmentEvents, this::isSnippet);
    appendWithDebugData(fragmentEvents, debugData);
  }

   void wrapWithFragmentId(List<FragmentEvent> fragmentEvents) {
    fragmentEvents.stream()
        .map(FragmentEvent::getFragment)
        .forEach(this::wrapFragmentBody);
  }
  void appendWithDebugData(List<FragmentEvent> fragmentEvents, JsonObject debugData){
        getFragmentWithBodyEndSection(fragmentEvents).ifPresent(appendWithDebugData(debugData));
  }
  private boolean isSnippet(FragmentEvent fragmentEvent){
    return "snippet".equals(fragmentEvent.getFragment().getType());
  }

  private Consumer<Fragment> appendWithDebugData(JsonObject debugData) {
    return fragment ->  fragment.setBody(fragment.getBody()
        .replace(BODY_SECTION_END,
            addAsScript("var debugData = " + debugData.encodePrettily() + ";")
                + addAsStyle(debugCss)
                + addAsScript(debugJs)
                + BODY_SECTION_END));
  }

  private void wrapFragmentBody(Fragment fragment) {
    fragment.setBody("<!-- data-knotx-id='" + fragment.getId() + "' -->"
        + fragment.getBody()
        + "<!-- data-knotx-id='" + fragment.getId() + "' -->");
  }

  private String addAsScript(String script) {
    return "<script>" + script + "</script>";
  }

  private String addAsStyle(String css) {
    return "<style>" + css + "</style>";
  }

  private Optional<Fragment> getFragmentWithBodyEndSection(
      List<FragmentEvent> fragmentEvents) {
    return fragmentEvents.stream()
        .filter(fragment -> fragment.getFragment()
            .getBody()
            .contains(BODY_SECTION_END))
        .findFirst()
        .map(FragmentEvent::getFragment);
  }
}