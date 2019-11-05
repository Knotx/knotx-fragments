package io.knotx.fragments.handler.action.cache;

import io.reactivex.Maybe;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.reactivex.redis.client.Response;
import io.vertx.redis.client.RedisOptions;
import java.util.Optional;

public class RedisCacheProvider implements CacheProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheProvider.class);

  private static final String DEFAULT_HOST = "localhost";

  private static final int DEFAULT_PORT = 6379;

  private static final long DEFAULT_TTL = 60;

  private final Redis redisClient;

  private final RedisAPI redis;

  private final long ttl;

  RedisCacheProvider(Vertx vertx, JsonObject config) {
    ttl = Optional.ofNullable(config.getJsonObject("cache"))
        .map(cacheConfig -> cacheConfig.getLong("ttl", DEFAULT_TTL))
        .orElse(DEFAULT_TTL);

    redisClient = Redis.createClient(io.vertx.reactivex.core.Vertx.newInstance(vertx), getRedisOptions(config));
    redis = RedisAPI.api(redisClient);
  }

  @Override
  public Maybe<Object> get(String key) {
    return redisClient.rxConnect()
        .flatMapMaybe(success -> redis.rxGet(key))
        .map(this::getObjectFromResponse);
  }

  @Override
  public void put(String key, Object value) {
    if (value instanceof JsonObject || value instanceof JsonArray) {
      String valueToBeCached = value.toString();
      redis.setex(key, Long.toString(ttl), valueToBeCached, response -> {
        if (response.succeeded()) {
          LOGGER.info("New value cached under key: {} for {} seconds", key, ttl);
        } else {
          LOGGER.error("Error while caching new value under key: {}", response.cause(), key);
        }
      });
    }
  }

  private RedisOptions getRedisOptions(JsonObject config) {
    return Optional.ofNullable(config.getJsonObject("redis"))
        .map(redisConfig -> {
          String host = redisConfig.getString("host", DEFAULT_HOST);
          int port = redisConfig.getInteger("port", DEFAULT_PORT);
          String password = redisConfig.getString("password", null);

          return new RedisOptions()
              .setEndpoint(SocketAddress.inetSocketAddress(port, host))
              .setPassword(password);
        })
        .orElseGet(RedisOptions::new);
  }

  private Object getObjectFromResponse(Response response) {
    return Optional.ofNullable(response)
        .map(Response::toString)
        .map(RedisCacheProvider::jsonToObject)
        .orElse(null);
  }

  private static Object jsonToObject(String json) {
    json = json.trim();
    if (json.startsWith("{")) {
      return new JsonObject(json);
    } else if (json.startsWith("[")) {
      return new JsonArray(json);
    } else {
      LOGGER.error("Couldn't deserialize string: {}", json);
      return null;
    }
  }
}