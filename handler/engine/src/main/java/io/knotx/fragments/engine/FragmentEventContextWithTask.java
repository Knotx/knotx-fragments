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
package io.knotx.fragments.engine;

import java.util.Optional;

public class FragmentEventContextWithTask implements FragmentEventContextTaskAware {

  private final Task task;
  private final FragmentEventContext fragmentEventContext;

  public FragmentEventContextWithTask(Task task, FragmentEventContext fragmentEventContext) {
    this.task = task;
    this.fragmentEventContext = fragmentEventContext;
  }

  @Override
  public Optional<Task> getTask() {
    return Optional.of(task);
  }

  @Override
  public FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }
}
