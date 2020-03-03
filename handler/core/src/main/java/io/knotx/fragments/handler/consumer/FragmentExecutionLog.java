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

import io.knotx.fragments.engine.EventLogEntry;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.TaskMetadata;
import io.knotx.fragments.handler.consumer.metadata.MetadataConverter;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Optional;

@DataObject(generateConverter = true)
public class FragmentExecutionLog {

  private String fragmentId;
  private String type;
  private Status status;
  private long startTime;
  private long finishTime;
  private JsonObject graph;

  private FragmentExecutionLog(FragmentEvent fragmentEvent, JsonObject graph) {
    this.fragmentId = fragmentEvent.getFragment().getId();
    this.type = fragmentEvent.getFragment().getType();
    this.status = fragmentEvent.getStatus();
    /* TODO: the timestamps calculated below are not consistent with the actual execution time
        To fix this, a change in TaskEngine/FragmentsHandler is required */
    this.startTime = fragmentEvent.getLog().getOperations().stream()
        .mapToLong(EventLogEntry::getTimestamp).min().orElse(0);
    this.finishTime = fragmentEvent.getLog().getOperations().stream()
        .mapToLong(EventLogEntry::getTimestamp).max().orElse(0);
    this.graph = graph;
  }

  public FragmentExecutionLog(JsonObject jsonObject) {
    FragmentExecutionLogConverter.fromJson(jsonObject, this);
  }

  public static FragmentExecutionLog from(FragmentEvent event, TaskMetadata taskMetadata) {
    return new FragmentExecutionLog(
        event,
        Optional.ofNullable(taskMetadata)
            .map(metadata -> MetadataConverter.from(event, metadata))
            .map(MetadataConverter::createJson)
            .orElseGet(JsonObject::new)
    );
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    FragmentExecutionLogConverter.toJson(this, json);
    return json;
  }

  public String getFragmentId() {
    return fragmentId;
  }

  public String getType() {
    return type;
  }

  public Status getStatus() {
    return status;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getFinishTime() {
    return finishTime;
  }

  public JsonObject getGraph() {
    return graph;
  }
}
