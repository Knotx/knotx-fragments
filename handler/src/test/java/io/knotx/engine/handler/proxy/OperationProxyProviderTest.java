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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.handler.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.engine.api.proxy.CacheableProxy;
import io.knotx.engine.api.proxy.FragmentOperation;
import io.knotx.engine.api.proxy.OperationProxyFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class OperationProxyProviderTest {

  private static final String PROXY_FACTORY_NAME = "testFactory";
  private static final String PROXY_FACTORY_NAME_SECOND = "testFactorySecond";
  private static final String PROXY_ALIAS = "proxyAlias";
  private static final String PROXY_ALIAS_SECOND = "proxyAliasSecond";

  @Test
  @DisplayName("Expect no operation when no proxy alias defined.")
  void getWithNoEntries(Vertx vertx) {
    // given
    OperationProxyProvider tested = new OperationProxyProvider(Collections.emptyMap(),
        Collections::emptyListIterator, vertx);

    // when
    Optional<FragmentOperation> operation = tested.get("any");

    // then
    assertFalse(operation.isPresent());
  }

  @Test
  @DisplayName("Expect no operation when no factory found.")
  void getWithNoFactory(Vertx vertx) {
    // given
    Map<String, OperationProxyFactoryOptions> proxies = Collections
        .singletonMap(PROXY_ALIAS, new OperationProxyFactoryOptions("eb", new JsonObject(), null));

    OperationProxyProvider tested = new OperationProxyProvider(proxies,
        Collections::emptyListIterator,
        vertx);

    // when
    Optional<FragmentOperation> operation = tested.get(PROXY_ALIAS);

    // then
    assertFalse(operation.isPresent());
  }

  @Test
  @DisplayName("Expect operation when proxy alias defined and factory found.")
  void getOperation(Vertx vertx) {
    // given
    Map<String, OperationProxyFactoryOptions> proxies = Collections
        .singletonMap(PROXY_ALIAS,
            new OperationProxyFactoryOptions(PROXY_FACTORY_NAME, new JsonObject(), null));
    List<OperationProxyFactory> factories = Collections
        .singletonList(new TestCacheableOperationFactory());

    OperationProxyProvider tested = new OperationProxyProvider(proxies,
        factories::iterator, vertx);

    // when
    Optional<FragmentOperation> operation = tested.get(PROXY_ALIAS);

    // then
    assertTrue(operation.isPresent());
  }

  @Test
  @DisplayName("Expect cached operation when call twice.")
  void getCachedOperation(Vertx vertx) {
    // given
    Map<String, OperationProxyFactoryOptions> proxies = Collections
        .singletonMap(PROXY_ALIAS,
            new OperationProxyFactoryOptions(PROXY_FACTORY_NAME, new JsonObject(), null));
    List<OperationProxyFactory> factories = Collections
        .singletonList(new TestCacheableOperationFactory());

    OperationProxyProvider tested = new OperationProxyProvider(proxies,
        factories::iterator, vertx);

    // when
    Optional<FragmentOperation> firstOperation = tested.get(PROXY_ALIAS);
    Optional<FragmentOperation> secondOperation = tested.get(PROXY_ALIAS);

    // then
    assertTrue(firstOperation.isPresent());
    assertTrue(secondOperation.isPresent());
    assertSame(firstOperation.get(), secondOperation.get());
  }

  @Test
  @DisplayName("Expect not empty next operation when chained proxy defined.")
  void getComplexOperation(Vertx vertx) {
    // given
    FragmentOperation expectedOperation = Mockito.mock(FragmentOperation.class);
    FragmentOperation expectedOperationSecond = Mockito.mock(FragmentOperation.class);

    OperationProxyFactory proxyFactory = Mockito.mock(OperationProxyFactory.class);
    when(proxyFactory.getName()).thenReturn(PROXY_FACTORY_NAME);
    when(proxyFactory
        .create(eq(PROXY_ALIAS), any(), eq(Optional.of(expectedOperationSecond)), eq(vertx)))
        .thenReturn(expectedOperation);

    OperationProxyFactory proxyFactorySecond = Mockito.mock(OperationProxyFactory.class);
    when(proxyFactorySecond.getName()).thenReturn(PROXY_FACTORY_NAME_SECOND);
    when(proxyFactorySecond.create(eq(PROXY_ALIAS_SECOND), any(), eq(Optional.empty()), eq(vertx)))
        .thenReturn(expectedOperationSecond);

    Map<String, OperationProxyFactoryOptions> proxies = ImmutableMap.of(
        PROXY_ALIAS,
        new OperationProxyFactoryOptions(PROXY_FACTORY_NAME, new JsonObject(), PROXY_ALIAS_SECOND),
        PROXY_ALIAS_SECOND,
        new OperationProxyFactoryOptions(PROXY_FACTORY_NAME_SECOND, new JsonObject())
    );
    List<OperationProxyFactory> factories = Arrays.asList(proxyFactory, proxyFactorySecond);

    OperationProxyProvider tested = new OperationProxyProvider(proxies, factories::iterator, vertx);

    // when
    tested.get(PROXY_ALIAS);

    // then
    Mockito.verify(proxyFactory)
        .create(eq(PROXY_ALIAS), any(), eq(Optional.of(expectedOperationSecond)), eq(vertx));
  }

  @CacheableProxy
  class TestCacheableOperationFactory implements OperationProxyFactory {

    @Override
    public String getName() {
      return PROXY_FACTORY_NAME;
    }

    @Override
    public FragmentOperation create(String alias, JsonObject config,
        Optional<FragmentOperation> nextProxy, Vertx vertx) {
      return (fragmentContext, resultHandler) -> {
        FragmentResult result = new FragmentResult(fragmentContext.getFragment(),
            FragmentResult.DEFAULT_TRANSITION);
        resultHandler.handle(Future.succeededFuture(result));
      };
    }
  }

}