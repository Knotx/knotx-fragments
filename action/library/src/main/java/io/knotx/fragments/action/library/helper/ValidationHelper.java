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
package io.knotx.fragments.action.library.helper;

import io.knotx.fragments.action.library.exception.ActionConfigurationException;

public final class ValidationHelper {

  private ValidationHelper() {
    // utility class
  }

  public static void checkArgument(String alias, boolean condition, String message) {
    if (condition) {
      throw new ActionConfigurationException(alias, message);
    }
  }
}