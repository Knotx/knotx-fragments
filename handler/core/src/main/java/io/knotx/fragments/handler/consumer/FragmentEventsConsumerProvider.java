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

import static java.util.stream.Collectors.toList;

import io.knotx.fragments.handler.api.exception.ConfigurationException;
import io.knotx.fragments.spi.FactoryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public class FragmentEventsConsumerProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FragmentEventsConsumerProvider.class);

  private final List<FragmentEventsConsumer> consumers;

  public FragmentEventsConsumerProvider(List<FactoryOptions> options) {
    ServiceLoader<FragmentEventsConsumerFactory> loader = ServiceLoader
        .load(FragmentEventsConsumerFactory.class);
    this.consumers = options.stream()
        .map(o -> StreamSupport.stream(loader.spliterator(), false)
            .filter(f -> f.getName().equals(o.getFactory()))
            .peek(f -> LOGGER.info("Registering fragment event consumer [{}]", f.getName()))
            .map(f -> f.create(o.getConfig()))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "Consumer factory [" + o.getFactory() + "] not configured!")))
        .collect(toList());
  }

  public List<FragmentEventsConsumer> provide() {
    return consumers;
  }

}
