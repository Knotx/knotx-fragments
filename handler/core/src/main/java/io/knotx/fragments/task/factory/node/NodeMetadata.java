package io.knotx.fragments.task.factory.node;

import io.knotx.fragments.spi.FactoryOptions;

public class NodeMetadata {

  private final FactoryOptions factoryOptions;

  public NodeMetadata(FactoryOptions factoryOptions) {
    this.factoryOptions = factoryOptions;
  }

  @Override
  public String toString() {
    return "NodeMetadata{" +
        "factoryOptions=" + factoryOptions +
        '}';
  }
}
