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
package io.knotx.fragments.action.cache.memory;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject(generateConverter = true)
public class InMemoryCacheOptions {

  private boolean enableMaximumSize = true;
  private boolean enableTtl = true;
  private boolean enableTtlAfterAccess = false;

  private int maximumSize = 1000;
  private int ttl = 5000;
  private int ttlAfterAccess = 5000;

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
   * @return true if maximumSize option is used, false otherwise
   */
  public boolean isEnableMaximumSize() {
    return enableMaximumSize;
  }

  /**
   * Enables/disables limiting number of allowed entries in cache.
   *
   * Defaults to true.
   *
   * @param enableMaximumSize true if maximumSize option is used, false otherwise
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setEnableMaximumSize(boolean enableMaximumSize) {
    this.enableMaximumSize = enableMaximumSize;
    return this;
  }

  /**
   * @return true if ttl (after write) option is used, false otherwise
   */
  public boolean isEnableTtl() {
    return enableTtl;
  }

  /**
   * Enables/disables automatic cache entry expiration after write (TTL).
   *
   * Defaults to true.
   *
   * @param enableTtl true if ttl (after write) option is used, false otherwise
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setEnableTtl(boolean enableTtl) {
    this.enableTtl = enableTtl;
    return this;
  }

  /**
   * @return true if ttlAfterAccess option is used, false otherwise
   */
  public boolean isEnableTtlAfterAccess() {
    return enableTtlAfterAccess;
  }

  /**
   * Enables/disables automatic cache entry expiration after access (TTL)
   *
   * Defaults to true.
   *
   * @param enableTtlAfterAccess true if enableTtlAfterAccess option is used, false otherwise
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setEnableTtlAfterAccess(boolean enableTtlAfterAccess) {
    this.enableTtlAfterAccess = enableTtlAfterAccess;
    return this;
  }

  /**
   * @return maximum number of entries present in cache
   */
  public int getMaximumSize() {
    return maximumSize;
  }

  /**
   * Sets the maximum cache size (maximum number of entries in cache). This option is used only if
   * {@link InMemoryCacheOptions#enableMaximumSize} flag is set.
   *
   * Defaults to 1000.
   *
   * @param maximumSize maximum number of entries present in cache
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setMaximumSize(int maximumSize) {
    this.maximumSize = maximumSize;
    return this;
  }

  /**
   * @return expire-after-write time in milliseconds
   */
  public int getTtl() {
    return ttl;
  }

  /**
   * Sets the expire-after-write time in milliseconds. This option is used only if {@link
   * InMemoryCacheOptions#enableTtl} flag is set.
   *
   * Defaults to 5000ms.
   *
   * @param ttl expire-after-write time in milliseconds
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setTtl(int ttl) {
    this.ttl = ttl;
    return this;
  }

  public int getTtlAfterAccess() {
    return ttlAfterAccess;
  }

  /**
   * Sets the expire-after-access time in milliseconds. This option is used only if {@link
   * InMemoryCacheOptions#enableTtlAfterAccess} flag is set.
   *
   * Defaults to 5000ms.
   *
   * @param ttlAfterAccess expire-after-access time in milliseconds
   * @return a reference to this, so the API can be used fluently
   */
  public InMemoryCacheOptions setTtlAfterAccess(int ttlAfterAccess) {
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
    return enableMaximumSize == that.enableMaximumSize &&
        enableTtl == that.enableTtl &&
        enableTtlAfterAccess == that.enableTtlAfterAccess &&
        maximumSize == that.maximumSize &&
        ttl == that.ttl &&
        ttlAfterAccess == that.ttlAfterAccess;
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(enableMaximumSize, enableTtl, enableTtlAfterAccess, maximumSize, ttl, ttlAfterAccess);
  }

  @Override
  public String toString() {
    return "InMemoryCacheOptions{" +
        "enableMaximumSize=" + enableMaximumSize +
        ", enableTtl=" + enableTtl +
        ", enableTtlAfterAccess=" + enableTtlAfterAccess +
        ", maximumSize=" + maximumSize +
        ", ttl=" + ttl +
        ", ttlAfterAccess=" + ttlAfterAccess +
        '}';
  }
}
