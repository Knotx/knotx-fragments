package io.knotx.fragments.handler.consumer;

public class JsonFragmentEventsConsumerFactory implements FragmentEventsConsumerFactory {

  @Override
  public String getName() {
    return "json";
  }

  @Override
  public FragmentEventsConsumer create() {
    return new JsonFragmentEventsConsumer();
  }
}