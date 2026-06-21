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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

/**
 * The logical value type of a {@link Schema}, independent of any backend or wire format. A {@code FieldType} knows its Java carrier type and how to narrow an arbitrary value to
 * it. Targets (protobuf, GraphQL, JSON Schema) map it to their own type system.
 *
 * <p>Numeric types are named by width ({@code int32}, {@code int64}, {@code float32}, {@code float64}) rather than by
 * their Java carrier. That naming is precise, keyword-safe, and aligned with cross-language wire vocabularies.
 *
 * @param <T> the Java value type this field carries.
 */
public final class FieldType<T> {

  public static final FieldType<String> STRING = new FieldType<>("string", String.class, "string", null);
  public static final FieldType<Integer> INT32 = new FieldType<>("int32", Integer.class, "integer", null);
  public static final FieldType<Long> INT64 = new FieldType<>("int64", Long.class, "integer", null);
  public static final FieldType<Float> FLOAT32 = new FieldType<>("float32", Float.class, "number", null);
  public static final FieldType<Double> FLOAT64 = new FieldType<>("float64", Double.class, "number", null);
  public static final FieldType<Boolean> BOOL = new FieldType<>("bool", Boolean.class, "boolean", null);
  public static final FieldType<Instant> TIMESTAMP = new FieldType<>("timestamp", Instant.class, "string", "date-time");
  public static final FieldType<UUID> UUID = new FieldType<>("uuid", UUID.class, "string", "uuid");
  public static final FieldType<byte[]> BYTES = new FieldType<>("bytes", byte[].class, "string", "byte");
  public static final FieldType<JsonArray> ARRAY = new FieldType<>("array", JsonArray.class, "array", null);
  public static final FieldType<JsonObject> OBJECT = new FieldType<>("object", JsonObject.class, "object", null);

  private final String typeName;
  private final Class<T> javaType;
  private final String jsonSchemaType;
  private final String jsonSchemaFormat;

  private FieldType(String typeName, Class<T> javaType, String jsonSchemaType, String jsonSchemaFormat) {
    this.typeName = typeName;
    this.javaType = javaType;
    this.jsonSchemaType = jsonSchemaType;
    this.jsonSchemaFormat = jsonSchemaFormat;
  }

  /**
   * A field type for a Java enum. Its carrier is {@code type} and it renders to JSON Schema as a {@code string}, the form an enum takes on the wire.
   *
   * @param type the enum class.
   * @param <E> the enum type.
   * @return a field type carrying {@code type}.
   */
  public static <E extends Enum<E>> FieldType<E> enumType(Class<E> type) {
    return new FieldType<>(type.getSimpleName(), type, "string", null);
  }

  public String typeName() {
    return typeName;
  }

  public Class<T> javaType() {
    return javaType;
  }

  public String jsonSchemaType() {
    return jsonSchemaType;
  }

  public String jsonSchemaFormat() {
    return jsonSchemaFormat;
  }

  /**
   * Narrows {@code value} to this field's Java type.
   *
   * @param value the value to narrow. A {@code null} passes through.
   * @return the value as {@code T}.
   * @throws ClassCastException if {@code value} is not assignable to {@link #javaType()}.
   */
  public T cast(Object value) {
    return value == null ? null : javaType.cast(value);
  }

  @Override
  public String toString() {
    return typeName;
  }
}
