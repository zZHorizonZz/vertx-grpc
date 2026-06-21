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
package io.vertx.schema.core;

import io.vertx.schema.FieldType;
import io.vertx.schema.Schema;

/**
 * The plain implementation for value types that need no type-specific refinement verbs ({@code bool}, {@code uuid}, {@code timestamp}, {@code bytes}). Universal modifiers
 * ({@code optional}, {@code defaultValue}, {@code refine}, {@code meta}) come from {@link Schema}.
 *
 * @param <T> the Java value type this schema validates.
 */
public final class CoreSchema<T> extends Schema<T, CoreSchema<T>> {

  CoreSchema(State<T> state) {
    super(state);
  }

  /**
   * Starts a required, refinement-free schema of the given type.
   *
   * @param type the value type.
   * @param <T> the Java value type.
   * @return a fresh schema.
   */
  public static <T> CoreSchema<T> of(FieldType<T> type) {
    return new CoreSchema<>(State.of(type));
  }

  @Override
  protected CoreSchema<T> withState(State<T> state) {
    return new CoreSchema<>(state);
  }
}
