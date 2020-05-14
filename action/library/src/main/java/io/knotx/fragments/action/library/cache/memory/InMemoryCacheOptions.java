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
package io.knotx.fragments.action.library.cache.memory;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject(generateConverter = true)
public class InMemoryCacheOptions {

  private Integer maximumSize = 1000;
  private Integer ttl = 5000;
  private Integer ttlAfterAccess;

  public InMemoryCacheOptions() {
  }

  public InMemoryCacheOptions(JsonObject json) {
    InMemoryCacheOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    InMemoryCacheOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * @return maximum number of entries present in cache
   */
  public Integer getMaximumSize() {
    return maximumSize;
  }

  /**
   * Sets the maximum cache size (maximum number of entries in cache). Defaults to 1000. If set to
   * null, cache will not be configured to use this option.
   *
   * @param maximumSize maximum number of entries present in cache
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setMaximumSize(Integer maximumSize) {
    this.maximumSize = maximumSize;
    return this;
  }

  /**
   * @return expire-after-write time in milliseconds
   */
  public Integer getTtl() {
    return ttl;
  }

  /**
   * Sets the expire-after-write time in milliseconds. Defaults to 5000ms. If set to null, cache
   * will not be configured to use this option.
   *
   * @param ttl expire-after-write time in milliseconds
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setTtl(Integer ttl) {
    this.ttl = ttl;
    return this;
  }

  public Integer getTtlAfterAccess() {
    return ttlAfterAccess;
  }

  /**
   * Sets the expire-after-access time in milliseconds. Defaults to null. If set to null, cache will
   * not be configured to use this option.
   *
   * @param ttlAfterAccess expire-after-access time in milliseconds
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setTtlAfterAccess(Integer ttlAfterAccess) {
    this.ttlAfterAccess = ttlAfterAccess;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InMemoryCacheOptions that = (InMemoryCacheOptions) o;
    return Objects.equals(maximumSize, that.maximumSize) &&
        Objects.equals(ttl, that.ttl) &&
        Objects.equals(ttlAfterAccess, that.ttlAfterAccess);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maximumSize, ttl, ttlAfterAccess);
  }

  @Override
  public String toString() {
    return "InMemoryCacheOptions{" +
        "maximumSize=" + maximumSize +
        ", ttl=" + ttl +
        ", ttlAfterAccess=" + ttlAfterAccess +
        '}';
  }
}
