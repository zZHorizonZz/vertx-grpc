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
import java.util.Map;
import java.util.Objects;

/**
 * A schema that accepts exactly one constant value. It validates as its carrier type carrying a {@code const} constraint, so {@code Schemas.literal("v")} accepts only
 * {@code "v"}.
 *
 * @param <T> the carrier type of the constant.
 */
public final class LiteralSchema<T> extends Schema<T, LiteralSchema<T>> {

  private final T literal;

  LiteralSchema(State<T> state, T literal) {
    super(state);
    this.literal = literal;
  }

  static <T> LiteralSchema<T> of(FieldType<T> type, T literal) {
    return new LiteralSchema<>(State.of(type), literal).check("invalid_literal", value -> Objects.equals(value, literal), "must be " + literal, Map.of("const", literal));
  }

  @Override
  protected LiteralSchema<T> withState(State<T> state) {
    return new LiteralSchema<>(state, literal);
  }

  /**
   * The single accepted value.
   *
   * @return the constant.
   */
  public T value() {
    return literal;
  }
}
