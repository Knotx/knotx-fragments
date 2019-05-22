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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.handler;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;


class HtmlFragmentsDebugModeDecorator {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HtmlFragmentsDebugModeDecorator.class);
  private static final String BODY_SECTION_END = "</body>";

  private String debugCss;
  private String debugJs;

  void init() {
    ClassLoader classLoader = getClass().getClassLoader();
    try (
        InputStream debugCssIs = classLoader.getResourceAsStream("debug/debug.css");
        InputStream debugJsIs = classLoader.getResourceAsStream("debug/debug.js")
    ) {
      debugCss = IOUtils.toString(debugCssIs, StandardCharsets.UTF_8);
      debugJs = IOUtils.toString(debugJsIs, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Failed to load file!", e);
    }
  }

  void appendFragmentBody(FragmentEvent fragmentEvent) {
    fragmentEvent.getDebugData().put("body", fragmentEvent.getFragment().getBody());
  }

  void addDebugAssetsAndData(List<FragmentEvent> fragmentEvents) {
    JsonObject debugData = new JsonObject();
    fragmentEvents.forEach(fragmentEvent -> {
      Fragment fragment = fragmentEvent.getFragment();
      appendFragmentPayload(fragmentEvent);
      wrapFragmentBody(fragment);
      debugData.put(fragment.getId(), fragmentEvent.getDebugData());
    });
    getFragmentWithBodyEndSection(fragmentEvents)
        .ifPresent(fragment -> fragment.setBody(fragment.getBody().replace(BODY_SECTION_END,
            addAsScript("var debugData = " + debugData.encodePrettily() + ";")
                + addAsStyle(debugCss)
                + addAsScript(debugJs)
                + BODY_SECTION_END)));
  }

  private String addAsScript(String script) {
    return "<script>" + script + "</script>";
  }

  private String addAsStyle(String css) {
    return "<style>" + css + "</style>";
  }

  private void appendFragmentPayload(FragmentEvent fragmentEvent) {
    fragmentEvent.getDebugData().put("payload", fragmentEvent.getFragment().getPayload());
  }

  private void wrapFragmentBody(Fragment fragment) {
    fragment.setBody("<!-- " + fragment.getId() + " -->"
        + fragment.getBody()
        + "<!-- " + fragment.getId() + " -->");

  }

  private Optional<Fragment> getFragmentWithBodyEndSection(
      List<FragmentEvent> fragmentEvents) {
    return fragmentEvents.stream()
        .filter(fragment -> fragment.getFragment().getBody().contains(BODY_SECTION_END))
        .findFirst()
        .map(FragmentEvent::getFragment);
  }
}
