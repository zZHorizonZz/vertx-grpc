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
import java.time.Instant;
import java.util.UUID;

/**
 * The entry point for building schemas, equivalent to zod's {@code z}. Each method starts a fresh, required schema of one type. Chain modifiers and refinements onto the result.
 * Numeric factories are named by width ({@link #int32()}, {@link #int64()}, {@link #float32()}, {@link #float64()}).
 *
 * <pre>{@code
 * StringSchema email = Schemas.string().min(3).email();
 * NumberSchema<Long> count = Schemas.int64().min(0L);
 * ObjectSchema user = Schemas.object()
 *     .field("id", Schemas.uuid())
 *     .field("email", Schemas.string().email())
 *     .field("tags", Schemas.array(Schemas.string()).max(10));
 * }</pre>
 */
public final class Schemas {

  private Schemas() {
  }

  /**
   * Starts a {@code String} schema, which adds the string refinements (length, regex, email, and so on).
   *
   * @return a new string schema.
   */
  public static StringSchema string() {
    return StringSchema.create();
  }

  /**
   * Starts a 32-bit signed integer schema carried by {@code Integer}, with the numeric bound refinements.
   *
   * @return a new int32 schema.
   */
  public static NumberSchema<Integer> int32() {
    return NumberSchema.create(FieldType.INT32);
  }

  /**
   * Starts a 64-bit signed integer schema carried by {@code Long}, with the numeric bound refinements.
   *
   * @return a new int64 schema.
   */
  public static NumberSchema<Long> int64() {
    return NumberSchema.create(FieldType.INT64);
  }

  /**
   * Starts a 32-bit floating-point schema carried by {@code Float}, with the numeric bound refinements.
   *
   * @return a new float32 schema.
   */
  public static NumberSchema<Float> float32() {
    return NumberSchema.create(FieldType.FLOAT32);
  }

  /**
   * Starts a 64-bit floating-point schema carried by {@code Double}, with the numeric bound refinements.
   *
   * @return a new float64 schema.
   */
  public static NumberSchema<Double> float64() {
    return NumberSchema.create(FieldType.FLOAT64);
  }

  /**
   * Starts a boolean schema.
   *
   * @return a new boolean schema.
   */
  public static CoreSchema<Boolean> bool() {
    return CoreSchema.of(FieldType.BOOL);
  }

  /**
   * Starts a timestamp schema carried by {@link Instant}.
   *
   * @return a new timestamp schema.
   */
  public static DateSchema timestamp() {
    return DateSchema.create();
  }

  /**
   * Starts a {@link UUID} schema.
   *
   * @return a new UUID schema.
   */
  public static CoreSchema<UUID> uuid() {
    return CoreSchema.of(FieldType.UUID);
  }

  /**
   * Starts a raw-bytes schema carried by {@code byte[]}.
   *
   * @return a new bytes schema.
   */
  public static CoreSchema<byte[]> bytes() {
    return CoreSchema.of(FieldType.BYTES);
  }

  /**
   * Starts a list schema whose elements are validated by {@code element}.
   *
   * @param element the schema applied to each element.
   * @param <E> the element type.
   * @return a new array schema.
   */
  public static <E> ArraySchema<E> array(Schema<E, ?> element) {
    return ArraySchema.of(element);
  }

  /**
   * Starts an object schema with no fields. Declare its shape with {@link ObjectSchema#field}.
   *
   * @return a new, empty object schema.
   */
  public static ObjectSchema object() {
    return ObjectSchema.create();
  }

  /**
   * Starts a string enumeration accepting only {@code values}.
   *
   * @param values the allowed values.
   * @return a new enum schema.
   */
  public static EnumSchema enumOf(String... values) {
    return EnumSchema.of(values);
  }

  /**
   * Starts an enum schema over the constants of {@code type}. It accepts an enum instance or a constant name and produces the constant.
   *
   * @param type the enum class.
   * @param <E> the enum type.
   * @return a new enum schema.
   */
  public static <E extends Enum<E>> EnumSchema<E> enumOf(Class<E> type) {
    return EnumSchema.of(type);
  }

  /**
   * Starts a schema accepting only the constant string {@code value}.
   *
   * @param value the only accepted value.
   * @return a new literal schema.
   */
  public static LiteralSchema<String> literal(String value) {
    return LiteralSchema.of(FieldType.STRING, value);
  }

  /**
   * Starts a schema accepting only the constant {@code int} {@code value}.
   *
   * @param value the only accepted value.
   * @return a new literal schema.
   */
  public static LiteralSchema<Integer> literal(int value) {
    return LiteralSchema.of(FieldType.INT32, value);
  }

  /**
   * Starts a schema accepting only the constant {@code boolean} {@code value}.
   *
   * @param value the only accepted value.
   * @return a new literal schema.
   */
  public static LiteralSchema<Boolean> literal(boolean value) {
    return LiteralSchema.of(FieldType.BOOL, value);
  }

  /**
   * Starts an open map schema: a {@code Map<String, V>} with arbitrary keys whose every value is validated by {@code value}.
   *
   * @param value the schema applied to each value.
   * @param <V> the value type.
   * @return a new record schema.
   */
  public static <V> RecordSchema<V> record(Schema<V, ?> value) {
    return RecordSchema.of(value);
  }
}
