package io.knotx.fragments.task.factory.node;

import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.NodeType;
import io.vertx.core.json.JsonObject;
import java.util.Optional;

public class MissingNode extends NodeWithMetadata {

  @Override
  public JsonObject generateMetadata() {
    return new JsonObject()
        .put("status", "missing");
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public <T extends Node> Optional<T> next(String transition) {
    return Optional.empty();
  }

  @Override
  public NodeType getType() {
    return null;
  }
}
