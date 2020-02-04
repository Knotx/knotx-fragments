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
package io.knotx.fragments.handler.action.http.request;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import java.lang.reflect.Field;

public class ResponsePredicatesProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponsePredicatesProvider.class);
  private static final Class PREDICATE_CLASS = ResponsePredicate.class;

  public ResponsePredicate fromName(String predicateName) {
    try {
      Field predicateField = PREDICATE_CLASS.getField(predicateName.toUpperCase());
      return (ResponsePredicate) predicateField.get(this);
    } catch (ReflectiveOperationException e) {
      LOGGER.error("Predicate with name {} does not exist", predicateName);
      throw new IllegalArgumentException(e.getMessage());
    }

  }
}
