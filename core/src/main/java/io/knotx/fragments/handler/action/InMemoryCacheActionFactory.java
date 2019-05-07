package io.knotx.fragments.handler.action;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.Cacheable;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

/**
 * Payload Cache Action factory class. It can be initialized with a configuration:
 * <pre>
 *   productDetails {
 *     name = in-memory-cache,
 *     config {
 *       cache {
 *         maximumSize = 1000
 *         ttl = 5000
 *       }
 *       key = product
 *     }
 *   }
 * </pre>
 */
@Cacheable
public class InMemoryCacheActionFactory implements ActionFactory {

  private static final long DEFAULT_MAXIMUM_SIZE = 1000;
  private static final long DEFAULT_TTL = 5000;


  @Override
  public String getName() {
    return "in-memory-cache";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {

    return new Action() {
      private Cache<String, Object> cache = createCache(config);
      private String key = getPayloadKey(config);

      @Override
      public void apply(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler) {

        Object cachedValue = cache.getIfPresent(key);
        if (cachedValue == null) {
          callDoActionAndCache(fragmentContext, resultHandler);
        } else {
          Fragment fragment = fragmentContext.getFragment();
          fragment.appendPayload(key, cachedValue);
          FragmentResult result = new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
          Future.succeededFuture(result).setHandler(resultHandler);
        }
      }

      private void callDoActionAndCache(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler) {
        doAction.apply(fragmentContext, asyncResult -> {
          if (asyncResult.succeeded()) {
            FragmentResult fragmentResult = asyncResult.result();
            if (FragmentResult.SUCCESS_TRANSITION.equals(fragmentResult.getTransition())
                && fragmentResult.getFragment().getPayload().containsKey(key)) {
              JsonObject resultPayload = fragmentResult.getFragment().getPayload();
              cache.put(key, resultPayload.getMap().get(key));
            }
            Future.succeededFuture(fragmentResult).setHandler(resultHandler);
          } else {
            Future.<FragmentResult>failedFuture(asyncResult.cause()).setHandler(resultHandler);
          }
        });
      }
    };
  }

  private String getPayloadKey(JsonObject config) {
    String result = config.getString("key");
    if (StringUtils.isBlank(result)) {
      throw new IllegalArgumentException("Action requires key value in configuration.");
    }
    return result;
  }

  private Cache<String, Object> createCache(JsonObject config) {
    JsonObject cache = config.getJsonObject("cache");
    long maxSize =
        cache == null ? DEFAULT_MAXIMUM_SIZE : cache.getLong("maximumSize", DEFAULT_MAXIMUM_SIZE);
    long ttl = cache == null ? DEFAULT_TTL : cache.getLong("ttl", DEFAULT_TTL);
    return CacheBuilder.newBuilder().maximumSize(maxSize)
        .expireAfterWrite(ttl, TimeUnit.MILLISECONDS).build();
  }
}
