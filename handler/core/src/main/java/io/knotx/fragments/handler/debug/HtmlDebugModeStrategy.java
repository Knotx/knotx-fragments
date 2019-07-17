package io.knotx.fragments.handler.debug;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class HtmlDebugModeStrategy implements FragmentsDebugModeStrategy {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(HtmlDebugModeStrategy.class);
  private static final String BODY_SECTION_END = "</body>";

  private final String debugCss;
  private final String debugJs;

  HtmlDebugModeStrategy() {
    this.debugCss = loadResourceToString("debug/debug.css");
    this.debugJs = loadResourceToString("debug/debug.js");
  }

  @Override
  public void updateBodyWithDebugData(JsonObject debugData, List<FragmentEvent> fragmentEvents) {
    wrapWithFragmentId(fragmentEvents);
    getFragmentWithBodyEndSection(fragmentEvents).ifPresent(appendWithDebugData(debugData));
  }

  private Consumer<Fragment> appendWithDebugData(JsonObject debugData) {
    return fragment ->  fragment.setBody(fragment.getBody()
        .replace(BODY_SECTION_END,
            addAsScript("var debugData = " + debugData.encodePrettily() + ";")
                + addAsStyle(debugCss)
                + addAsScript(debugJs)
                + BODY_SECTION_END));
  }

  private void wrapWithFragmentId(List<FragmentEvent> fragmentEvents) {
    fragmentEvents.stream()
        .map(FragmentEvent::getFragment)
        .forEach(this::wrapFragmentBody);
  }

  private String loadResourceToString(String path) {
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(path)) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(format("Failed to load file %s!", path), e);
    }
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
