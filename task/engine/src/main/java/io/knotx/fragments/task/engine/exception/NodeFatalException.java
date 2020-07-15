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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.task.engine.exception;

import io.knotx.fragments.api.Fragment;

public class NodeFatalException extends IllegalStateException {

  private Fragment fragment;

  public NodeFatalException(Fragment fragment) {
    super("Failed during fragment processing [" + fragment.getId() + "]");
    this.fragment = fragment;
  }

  public Fragment getFragment() {
    return fragment;
  }
}
