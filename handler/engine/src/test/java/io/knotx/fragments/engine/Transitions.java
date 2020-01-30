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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.engine.graph.Node;
import java.util.Collections;
import java.util.Map;

public interface Transitions {

  static Map<String, Node> onSuccess(Node node) {
    return Collections.singletonMap(SUCCESS_TRANSITION, node);
  }

  static Map<String, Node> onError(Node node) {
    return Collections.singletonMap(ERROR_TRANSITION, node);
  }

}
