package io.knotx.fragments.handler.debug;

import static io.knotx.fragments.api.Fragment.JSON_OBJECT_TYPE;

import java.util.List;

import com.google.common.base.Preconditions;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;

public class JsonObjectDebugModeStrategy implements FragmentsDebugModeStrategy{

  @Override
  public void updateBodyWithDebugData(JsonObject debugData, List<FragmentEvent> fragmentEvents) {
    Preconditions.checkArgument(fragmentEvents.size() == 1, "Only one fragment expected");
    Fragment fragment = fragmentEvents.get(0).getFragment();
    Preconditions.checkArgument(JSON_OBJECT_TYPE.equals(fragment.getType()));

    JsonObject body = new JsonObject(fragment.getBody());
    body.put("debug", debugData);

    fragment.setBody(body.encode());
  }
}
