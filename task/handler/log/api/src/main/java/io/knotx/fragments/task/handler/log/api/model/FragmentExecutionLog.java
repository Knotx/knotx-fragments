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
package io.knotx.fragments.handler.consumer.api.model;

import io.knotx.fragments.api.Fragment;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Contains detailed information about fragment processing.
 */
@DataObject(generateConverter = true)
public class FragmentExecutionLog {

  private Fragment fragment;
  private ExecutionStatus status = ExecutionStatus.UNPROCESSED;
  private long startTime = 0;
  private long finishTime = 0;
  private GraphNodeExecutionLog graph = null;

  public static FragmentExecutionLog newInstance(Fragment fragment) {
    return newInstance(fragment, ExecutionStatus.UNPROCESSED, 0, 0);
  }

  public static FragmentExecutionLog newInstance(Fragment fragment, GraphNodeExecutionLog graph) {
    return newInstance(fragment, ExecutionStatus.UNPROCESSED, 0, 0, graph);
  }

  public static FragmentExecutionLog newInstance(Fragment fragment, ExecutionStatus status,
      long startTime, long finishTime) {
    return newInstance(fragment, status, startTime, finishTime, null);
  }

  public static FragmentExecutionLog newInstance(Fragment fragment, ExecutionStatus status,
      long startTime, long finishTime,
      GraphNodeExecutionLog graph) {
    return new FragmentExecutionLog()
        .setFragment(fragment)
        .setStatus(status)
        .setStartTime(startTime)
        .setFinishTime(finishTime)
        .setGraph(graph);
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

  /**
   * <a href="https://github.com/Knotx/knotx-fragments/blob/master/api/docs/asciidoc/dataobjects.adoc#fragment">Fragment</a>
   * details.
   *
   * @return fragment
   */
  public Fragment getFragment() {
    return fragment;
  }

  public FragmentExecutionLog setFragment(Fragment fragment) {
    this.fragment = fragment;
    return this;
  }

  /**
   * Possible values: <code>UNPROCESSED</code>, <code>SUCCESS</code> and <code>FAILURE</code>.
   *
   * @return fragment status.
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  public FragmentExecutionLog setStatus(ExecutionStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Node processing start time. It is timestamp value.
   *
   * @return node processing start time
   */
  public long getStartTime() {
    return startTime;
  }

  public FragmentExecutionLog setStartTime(long startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Node processing finish time. It is timestamp value.
   *
   * @return node processing finish time
   */
  public long getFinishTime() {
    return finishTime;
  }

  public FragmentExecutionLog setFinishTime(long finishTime) {
    this.finishTime = finishTime;
    return this;
  }

  /**
   * Task evaluation details.
   *
   * @return processing details of the root graph node
   */
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

  public enum ExecutionStatus {
    UNPROCESSED,
    SUCCESS,
    FAILURE;
  }
}
