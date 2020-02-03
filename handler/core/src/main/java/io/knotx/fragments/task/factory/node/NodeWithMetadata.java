package io.knotx.fragments.task.factory.node;

import io.knotx.fragments.engine.graph.Node;
import io.vertx.core.json.JsonObject;

public interface NodeWithMetadata extends Node {

  JsonObject getData();

}
