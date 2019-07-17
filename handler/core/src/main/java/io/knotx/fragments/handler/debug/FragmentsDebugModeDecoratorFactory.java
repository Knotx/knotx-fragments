package io.knotx.fragments.handler.debug;

import static io.knotx.fragments.api.Fragment.JSON_OBJECT_TYPE;
import static io.knotx.fragments.api.Fragment.SNIPPET_TYPE;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FragmentsDebugModeDecoratorFactory {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FragmentsDebugModeDecoratorFactory.class);
  private static final String FRAGMENT_TYPE_NOT_SUPPORTED = "NOT_SUPPORTED";
  private final boolean debugMode;

  private final Map<String, FragmentsDebugModeStrategy> strategies;

  public FragmentsDebugModeDecoratorFactory(boolean debugMode) {
    this.debugMode = debugMode;

    if (debugMode) {
      strategies = ImmutableMap.<String, FragmentsDebugModeStrategy>builder()
          .put(SNIPPET_TYPE, new HtmlDebugModeStrategy())
          .put(JSON_OBJECT_TYPE, new JsonObjectDebugModeStrategy())
          .put(FRAGMENT_TYPE_NOT_SUPPORTED, new NotSupportedDebugModeStrategy())
          .build();

    } else {
      strategies = ImmutableMap.<String, FragmentsDebugModeStrategy>builder()
          .build();
    }
  }

  public FragmentsDebugModeDecorator create(List<FragmentEventContextTaskAware> events, boolean debugMode) {
    return new FragmentsDebugModeDecorator(getStrategy(events), debugMode);
  }

  private FragmentsDebugModeStrategy getStrategy(List<FragmentEventContextTaskAware> events) {
    return strategies.get(getFragmentsMasterType(events));

  }

  private String getFragmentsMasterType(List<FragmentEventContextTaskAware> events){
    if(hasAnyFragmentType(events, SNIPPET_TYPE)){
      return SNIPPET_TYPE;
    }

    if(isJsonObject(events)){
      return JSON_OBJECT_TYPE;
    }

    return FRAGMENT_TYPE_NOT_SUPPORTED;
  }

  private boolean hasAnyFragmentType(List<FragmentEventContextTaskAware> events, String type){
    return events.stream().anyMatch(event -> type
        .equals(event.getFragmentEventContext().getFragmentEvent().getFragment().getType()));
  }

  private boolean isJsonObject(List<FragmentEventContextTaskAware> events){
    return events.size() == 1 && hasAnyFragmentType(events, JSON_OBJECT_TYPE);
  }
}