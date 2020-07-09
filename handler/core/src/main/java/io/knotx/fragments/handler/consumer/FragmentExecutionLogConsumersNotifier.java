/*
 * Copyright (C) 2019 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.knotx.fragments.handler.consumer;

import static io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog.newInstance;
import static java.util.stream.Collectors.toList;

import io.knotx.fragments.engine.api.FragmentEvent;
import io.knotx.fragments.handler.ExecutionPlan;
import io.knotx.fragments.handler.api.exception.ConfigurationException;
import io.knotx.fragments.handler.api.metadata.TasksMetadata;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumerFactory;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.fragments.spi.FactoryOptions;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FragmentExecutionLogConsumersNotifier {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FragmentExecutionLogConsumersNotifier.class);

  private final List<FragmentExecutionLogConsumer> consumers;

  public FragmentExecutionLogConsumersNotifier(List<FactoryOptions> consumerOptionsList) {
    ServiceLoader<FragmentExecutionLogConsumerFactory> loader = ServiceLoader
        .load(FragmentExecutionLogConsumerFactory.class);
    this.consumers = consumerOptionsList.stream()
        .map(consumerOptions -> initConsumer(loader, consumerOptions))
        .collect(toList());
  }

  private FragmentExecutionLogConsumer initConsumer(
      ServiceLoader<FragmentExecutionLogConsumerFactory> loader, FactoryOptions consumerOptions) {
    return StreamSupport.stream(loader.spliterator(), false)
        .filter(f -> f.getName().equals(consumerOptions.getFactory()))
        .peek(f -> LOGGER.info("Registering fragment event consumer [{}]", f.getName()))
        .map(f -> f.create(consumerOptions.getConfig()))
        .findFirst()
        .orElseThrow(() -> new ConfigurationException(
            "Consumer factory [" + consumerOptions.getFactory() + "] not configured!"));
  }


  public void notify(ClientRequest clientRequest, List<FragmentEvent> events,
      ExecutionPlan executionPlan) {
    TasksMetadata tasksMetadata = executionPlan.getTasksMetadata();
    List<FragmentExecutionLog> executionDataList = events.stream()
        .map(e -> convert(e, tasksMetadata))
        .collect(Collectors.toList());
    consumers
        .forEach(consumer -> consumer.accept(clientRequest, executionDataList));
  }

  private FragmentExecutionLog convert(FragmentEvent event, TasksMetadata tasksMetadata) {
    return
        Optional.ofNullable(tasksMetadata.get(event.getFragment().getId()))
            .map(metadata -> new MetadataConverter(event, metadata))
            .map(MetadataConverter::getExecutionLog)
            .map(graphLog -> newInstance(event, graphLog))
            .orElseGet(() -> newInstance(event));
  }
}