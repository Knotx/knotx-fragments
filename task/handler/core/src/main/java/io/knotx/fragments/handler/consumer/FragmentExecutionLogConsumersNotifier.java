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

import static io.knotx.fragments.task.handler.log.api.model.FragmentExecutionLog.newInstance;
import static java.util.stream.Collectors.toList;

import io.knotx.fragments.task.engine.FragmentEvent;
import io.knotx.fragments.handler.ExecutionPlan;
import io.knotx.fragments.task.handler.log.api.model.FragmentExecutionLog.ExecutionStatus;
import io.knotx.fragments.handler.exception.ConfigurationException;
import io.knotx.fragments.task.factory.api.metadata.TasksMetadata;
import io.knotx.fragments.task.handler.log.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.task.handler.log.api.FragmentExecutionLogConsumerFactory;
import io.knotx.fragments.task.handler.log.api.model.FragmentExecutionLog;
import io.knotx.fragments.handler.spi.FactoryOptions;
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
    LOGGER.info("Registered consumers [{}]", consumers);
  }

  private FragmentExecutionLogConsumer initConsumer(
      ServiceLoader<FragmentExecutionLogConsumerFactory> loader, FactoryOptions consumerOptions) {
    return StreamSupport.stream(loader.spliterator(), false)
        .filter(f -> f.getName().equals(consumerOptions.getFactory()))
        .peek(f -> LOGGER
            .info("Registering consumer [{}] with name [{}] with config [{}].", f.getClass(),
                f.getName(), consumerOptions.getConfig()))
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
    LOGGER.trace("Notify consumers with execution data [{}]", executionDataList);
    consumers
        .forEach(consumer -> consumer.accept(clientRequest, executionDataList));
  }

  private FragmentExecutionLog convert(FragmentEvent event, TasksMetadata tasksMetadata) {
    return
        Optional.ofNullable(tasksMetadata.get(event.getFragment().getId()))
            .map(metadata -> new MetadataConverter(event, metadata))
            .map(MetadataConverter::getExecutionLog)
            .map(graphLog -> newInstance(event.getFragment(),
                toExecutionStatus(event),
                event.getLog().getEarliestTimestamp(),
                event.getLog().getLatestTimestamp(),
                graphLog))
            .orElseGet(() -> newInstance(event.getFragment()));
  }

  private ExecutionStatus toExecutionStatus(FragmentEvent event) {
    return ExecutionStatus.valueOf(event.getStatus().toString());
  }
}
