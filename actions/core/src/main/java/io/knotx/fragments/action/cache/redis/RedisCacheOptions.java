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
package io.knotx.fragments.action.cache.redis;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisOptions;
import java.util.Objects;

@DataObject(generateConverter = true)
public class RedisCacheOptions {

  private Long ttl;
  private RedisOptions redis;

  public RedisCacheOptions() {
  }

  public RedisCacheOptions(JsonObject json) {
    RedisCacheOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    RedisCacheOptionsConverter.toJson(this, json);
    return json;
  }

  public Long getTtl() {
    return ttl;
  }

  public RedisCacheOptions setTtl(Long ttl) {
    this.ttl = ttl;
    return this;
  }

  public RedisOptions getRedis() {
    return redis;
  }

  public RedisCacheOptions setRedis(RedisOptions redis) {
    this.redis = redis;
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
    RedisCacheOptions that = (RedisCacheOptions) o;
    return Objects.equals(ttl, that.ttl) &&
        Objects.equals(redis, that.redis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ttl, redis);
  }

  @Override
  public String toString() {
    return "RedisCacheOptions{" +
        "ttl=" + ttl +
        ", redis=" + redis +
        '}';
  }
}
