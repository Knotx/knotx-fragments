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
package io.knotx.fragments.task.engine;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.task.api.Task;
import io.knotx.server.api.context.ClientRequest;

public class FragmentContextTaskAware {

  private final Task task;
  private final FragmentContext fragmentContext;

  public FragmentContextTaskAware(Task task,
      ClientRequest clientRequest, Fragment fragment) {
    this.task = task;
    this.fragmentContext = new FragmentContext(fragment, clientRequest);
  }

  public Task getTask() {
    return task;
  }

  public FragmentContext getFragmentContext() {
    return fragmentContext;
  }
}
