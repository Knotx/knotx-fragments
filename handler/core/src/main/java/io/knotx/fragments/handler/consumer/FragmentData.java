package io.knotx.fragments.handler.consumer;

import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class FragmentData {

  private String id;
  private String type;

  public FragmentData(FragmentEvent fragmentEvent) {
    this.id = fragmentEvent.getFragment().getId();
    this.type = fragmentEvent.getFragment().getType();
  }

  public FragmentData(JsonObject jsonObject) {
    FragmentDataConverter.fromJson(jsonObject, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    FragmentDataConverter.toJson(this, json);
    return json;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }
}
