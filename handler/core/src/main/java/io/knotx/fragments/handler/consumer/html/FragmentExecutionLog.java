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
package io.knotx.fragments.handler.consumer.html;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class FragmentExecutionLog {

  private Fragment fragment;
  private FragmentEvent.Status status = Status.UNPROCESSED;
  private long startTime = 0;
  private long finishTime = 0;
  private GraphNodeExecutionLog graph = null;

  public static FragmentExecutionLog newInstance(FragmentEvent fragmentEvent,
      GraphNodeExecutionLog graph) {
    return newInstance(fragmentEvent)
        .setGraph(graph);
  }

  public static FragmentExecutionLog newInstance(FragmentEvent fragmentEvent) {
    return new FragmentExecutionLog()
        .setFragment(fragmentEvent.getFragment())
        .setStatus(fragmentEvent.getStatus())
        .setStartTime(fragmentEvent.getLog().getEarliestTimestamp())
        .setFinishTime(fragmentEvent.getLog().getLatestTimestamp());
  }

  public FragmentExecutionLog() {
    // default constructor
  }

  public FragmentExecutionLog(JsonObject jsonObject) {
    FragmentExecutionLogConverter.fromJson(jsonObject, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    FragmentExecutionLogConverter.toJson(this, json);
    return json;
  }

  public Fragment getFragment() {
    return fragment;
  }

  public FragmentExecutionLog setFragment(Fragment fragment) {
    this.fragment = fragment;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public FragmentExecutionLog setStatus(Status status) {
    this.status = status;
    return this;
  }

  public long getStartTime() {
    return startTime;
  }

  public FragmentExecutionLog setStartTime(long startTime) {
    this.startTime = startTime;
    return this;
  }

  public long getFinishTime() {
    return finishTime;
  }

  public FragmentExecutionLog setFinishTime(long finishTime) {
    this.finishTime = finishTime;
    return this;
  }

  public GraphNodeExecutionLog getGraph() {
    return graph;
  }

  public FragmentExecutionLog setGraph(
      GraphNodeExecutionLog graph) {
    this.graph = graph;
    return this;
  }

  @Override
  public String toString() {
    return "FragmentExecutionLog{" +
        "status=" + status +
        ", startTime=" + startTime +
        ", finishTime=" + finishTime +
        ", fragment=" + fragment +
        ", graph=" + graph +
        '}';
  }
}
