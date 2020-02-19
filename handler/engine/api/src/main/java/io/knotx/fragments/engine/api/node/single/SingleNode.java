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
package io.knotx.fragments.engine.api.node.single;

import io.knotx.fragments.engine.api.node.Node;
import io.knotx.fragments.engine.api.node.NodeType;
import io.reactivex.Single;

public interface SingleNode extends Node {

  Single<FragmentResult> execute(FragmentContext fragmentContext);

  @Override
  default NodeType getType() {
    return NodeType.SINGLE;
  }

}
