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

import io.vertx.core.json.JsonObject;
import io.vertx.schema.*;
import io.vertx.schema.standard.v1.StandardJSONSchemaV1;
import java.util.List;
import java.util.Map;

/**
 * A schema for an open map, a {@link JsonObject} with arbitrary keys whose every value is validated by the same schema. It reports a bad value at its key's path (so a problem
 * under key {@code "a"} reads as {@code "a"}) and produces a new object of the validated values. Maps to JSON Schema {@code additionalProperties}.
 *
 * @param <V> the value type.
 */
public final class RecordSchema<V> extends Schema<JsonObject, RecordSchema<V>> {

  private final Schema<V, ?> valueSchema;

  RecordSchema(State<JsonObject> state, Schema<V, ?> valueSchema) {
    super(state);
    this.valueSchema = valueSchema;
  }

  @SuppressWarnings("unchecked")
  static <V> RecordSchema<V> of(Schema<V, ?> valueSchema) {
    FieldType<JsonObject> type = (FieldType<JsonObject>) (FieldType<?>) FieldType.OBJECT;
    return new RecordSchema<>(State.of(type), valueSchema);
  }

  @Override
  protected RecordSchema<V> withState(State<JsonObject> state) {
    return new RecordSchema<>(state, valueSchema);
  }

  /**
   * The schema each value is validated against.
   *
   * @return the value schema.
   */
  public Schema<V, ?> valueSchema() {
    return valueSchema;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected JsonObject coerce(Object input) {
    if (input instanceof JsonObject) {
      return (JsonObject) input;
    }
    if (input instanceof Map) {
      return new JsonObject((Map<String, Object>) input);
    }
    throw new ClassCastException();
  }

  @Override
  protected JsonObject process(JsonObject value, SchemaPath path, List<SchemaIssue> issues) {
    JsonObject out = new JsonObject();
    for (Map.Entry<String, Object> entry : value.getMap().entrySet()) {
      ParseResult<V> result = valueSchema.safeParse(entry.getValue(), path.field(entry.getKey()));
      if (result.isSuccess()) {
        out.put(entry.getKey(), result.value().orElse(null));
      } else {
        issues.addAll(result.issues());
      }
    }
    return out;
  }

  @Override
  protected Map<String, Object> jsonSchemaBody(StandardJSONSchemaV1.Options options, boolean input) {
    Map<String, Object> node = super.jsonSchemaBody(options, input);
    node.put("additionalProperties", input ? valueSchema.standard().jsonSchema().input(options) : valueSchema.standard().jsonSchema().output(options));
    return node;
  }
}
