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
package io.knotx.fragments.task;

import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.FragmentsHandlerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

/**
 * A task factory interface allowing to register a task factory by its name. Implementing class must
 * be configured in <code>META-INF.services</code>.
 *
 * Task factories are configured in {@link FragmentsHandlerOptions#getTaskFactories()}.
 */
public interface TaskFactory {

  /**
   * @return task factory name
   */
  String getName();

  /**
   * Configures a task factory with config defined in {@link TaskFactoryOptions#getConfig()}. This
   * method is called during factories initialization.
   *
   * @param config json configuration, see {@link TaskFactoryOptions#getConfig()}
   * @param vertx vertx instance
   * @return a reference to this, so the API can be used fluently
   */
  TaskFactory configure(JsonObject config, Vertx vertx);

  /**
   * Determines if a fragment event context can be processed by the factory.
   *
   * @param context fragment event context
   * @return <code>true</code> when accepted
   */
  boolean accept(FragmentEventContext context);

  /**
   * Creates the new task instance. It is called only if {@link #accept(FragmentEventContext)}
   * returns <code>true</code>. When called with a fragment that does not provide a task name, then
   * {@link io.knotx.fragments.task.exception.TaskNotFoundException} is thrown.
   *
   * @param context fragment event context
   * @return new task instance
   */
  Task newInstance(FragmentEventContext context);
}
