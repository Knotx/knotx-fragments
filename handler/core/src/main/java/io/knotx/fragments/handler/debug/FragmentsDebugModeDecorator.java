package io.knotx.fragments.handler.debug;

import java.util.List;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.vertx.core.json.JsonObject;

public class FragmentsDebugModeDecorator {

  private final FragmentsDebugModeStrategy strategy;
  private final boolean debugMode;

  FragmentsDebugModeDecorator(
      FragmentsDebugModeStrategy strategy, boolean debugMode) {
    this.strategy = strategy;
    this.debugMode = debugMode;
  }

  public void markAsDebuggable(List<FragmentEventContextTaskAware> events) {
    if(debugMode) {
      events.stream().filter(this::hasTask).forEach(this::markAsDebuggable);
    }
  }

  public void addDebugAssetsAndData(List<FragmentEvent> fragmentEvents) {
    if (debugMode) {
      JsonObject debugData = new JsonObject();
      fragmentEvents.stream()
          .filter(this::isDebugged)
          .forEach(fragmentEvent -> {
            Fragment fragment = fragmentEvent.getFragment();
            appendFragmentPayload(fragmentEvent);
            appendFragmentLog(fragmentEvent);
            debugData.put(fragment.getId(), fragmentEvent.getDebugData());
          });

      strategy.updateBodyWithDebugData(debugData, fragmentEvents);
    }
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
}
