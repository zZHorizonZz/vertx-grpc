/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.schema;

import java.util.Map;
import java.util.function.Predicate;

/**
 * A single validation rule applied to an already-typed value. Built-in refinements ({@code min}, {@code max}, {@code regex}) and user
 * {@link Schema#refine(Predicate, String) refine} rules are both expressed as checks.
 *
 * <p>A check may also contribute a JSON Schema fragment via {@link #jsonSchema()}, the keywords that describe the same
 * constraint it validates (for example {@code {"minLength": 3}} for a string minimum). Declaring the constraint once keeps validation and JSON Schema conversion in sync.
 *
 * @param <T> the value type this check validates.
 */
public interface Check<T> {

  /**
   * Builds a check from a predicate, contributing no JSON Schema fragment.
   *
   * @param code the failure code to report.
   * @param predicate returns {@code true} when the value is valid.
   * @param message the failure message when the predicate returns {@code false}.
   * @param <T> the value type.
   * @return a check.
   */
  static <T> Check<T> of(String code, Predicate<? super T> predicate, String message) {
    return of(code, predicate, message, Map.of());
  }

  /**
   * Builds a check from a predicate, contributing {@code jsonSchema} keywords that describe the same constraint.
   *
   * @param code the failure code to report.
   * @param predicate returns {@code true} when the value is valid.
   * @param message the failure message when the predicate returns {@code false}.
   * @param jsonSchema the JSON Schema keywords this constraint contributes.
   * @param <T> the value type.
   * @return a check.
   */
  static <T> Check<T> of(String code, Predicate<? super T> predicate, String message, Map<String, Object> jsonSchema) {
    Map<String, Object> fragment = Map.copyOf(jsonSchema);
    return new Check<T>() {
      @Override
      public String validate(T value) {
        return predicate.test(value) ? null : message;
      }

      @Override
      public String code() {
        return code;
      }

      @Override
      public Map<String, Object> jsonSchema() {
        return fragment;
      }
    };
  }

  /**
   * Validates {@code value}.
   *
   * @param value the value to check, never {@code null}. Null-handling happens before checks run.
   * @return {@code null} if the value passes, otherwise the failure message.
   */
  String validate(T value);

  default String code() {
    return "custom";
  }

  default Map<String, Object> jsonSchema() {
    return Map.of();
  }
}
