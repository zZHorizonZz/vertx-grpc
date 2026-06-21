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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A schema for a closed set of values, either a set of string literals ({@code Schemas.enumOf("A", "B")}) or the constants of a Java {@code enum}
 * ({@code Schemas.enumOf(MyEnum.class)}). The Java-enum form also accepts a constant's name and resolves it to the constant. Either way it validates as a {@code string} carrying
 * an {@code enum} constraint, and its allowed values are available through {@link #values()} for a target to map to its own enum construct.
 *
 * @param <T> the value type, {@code String} for a string enum or the enum type for a Java enum.
 */
public final class EnumSchema<T> extends Schema<T, EnumSchema<T>> {

  private final List<T> values;
  private final Function<Object, T> narrow;

  EnumSchema(State<T> state, List<T> values, Function<Object, T> narrow) {
    super(state);
    this.values = values;
    this.narrow = narrow;
  }

  static EnumSchema<String> of(String... values) {
    List<String> allowed = List.of(values);
    Function<Object, String> narrow = input -> input instanceof String ? (String) input : null;
    return new EnumSchema<>(State.of(FieldType.STRING), allowed, narrow).check("invalid_enum_value", allowed::contains, "must be one of " + allowed, Map.of("enum", allowed));
  }

  static <E extends Enum<E>> EnumSchema<E> of(Class<E> type) {
    List<E> allowed = List.of(type.getEnumConstants());
    List<String> names = allowed.stream().map(Enum::name).collect(Collectors.toList());
    Function<Object, E> narrow = input -> {
      if (type.isInstance(input)) {
        return type.cast(input);
      }
      if (input instanceof String) {
        return resolve(type, (String) input);
      }
      return null;
    };
    return new EnumSchema<>(State.of(FieldType.enumType(type)), allowed, narrow).check("invalid_enum_value", allowed::contains, "must be one of " + names, Map.of("enum", names));
  }

  private static <E extends Enum<E>> E resolve(Class<E> type, String name) {
    try {
      return Enum.valueOf(type, name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  protected EnumSchema<T> withState(State<T> state) {
    return new EnumSchema<>(state, values, narrow);
  }

  /**
   * The allowed values, in declaration order.
   *
   * @return the enumeration's values.
   */
  public List<T> values() {
    return values;
  }

  @Override
  protected T coerce(Object input) {
    T value = narrow.apply(input);
    if (value == null) {
      throw new ClassCastException();
    }
    return value;
  }
}
