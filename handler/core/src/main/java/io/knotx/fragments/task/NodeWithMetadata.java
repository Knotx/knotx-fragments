package io.knotx.fragments.task;

import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.task.factory.node.NodeMetadata;

public interface NodeWithMetadata extends Node {

  NodeMetadata getMetadata();

}
