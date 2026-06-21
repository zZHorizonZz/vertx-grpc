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
import java.util.*;

/**
 * A schema for objects, a named set of fields each described by its own schema. It validates a {@link JsonObject}, running every field's schema against the matching entry
 * and reporting a nested problem at a dotted path such as {@code "address.zip"}. Validation returns a new object of the validated field values, and the input object is not modified.
 *
 * <p>The field set can be reshaped with the usual object operations ({@link #extend}, {@link #pick}, {@link #omit}, and
 * {@link #partial}), each returning a new schema. Unknown keys (present in the input but not declared as fields) are dropped by default. {@link #strict()} turns them into a
 * validation error, and {@link #passthrough()} keeps them in the output untouched.
 */
public final class ObjectSchema extends Schema<JsonObject, ObjectSchema> {

  private final Map<String, Schema<?, ?>> fields;
  private final boolean strict;
  private final boolean passthrough;

  ObjectSchema(State<JsonObject> state, Map<String, Schema<?, ?>> fields, boolean strict, boolean passthrough) {
    super(state);
    this.fields = fields;
    this.strict = strict;
    this.passthrough = passthrough;
  }

  @SuppressWarnings("unchecked")
  static ObjectSchema create() {
    FieldType<JsonObject> type = (FieldType<JsonObject>) (FieldType<?>) FieldType.OBJECT;
    return new ObjectSchema(State.of(type), new LinkedHashMap<>(), false, false);
  }

  @Override
  protected ObjectSchema withState(State<JsonObject> state) {
    return new ObjectSchema(state, fields, strict, passthrough);
  }

  /**
   * The declared fields and their schemas, in declaration order.
   *
   * @return an unmodifiable view of the fields.
   */
  public Map<String, Schema<?, ?>> fields() {
    return Collections.unmodifiableMap(fields);
  }

  /**
   * Adds a field, or replaces it if a field of the same name already exists.
   *
   * @param name the field name.
   * @param schema the schema validating that field's value.
   * @return a new object schema including the field.
   */
  public ObjectSchema field(String name, Schema<?, ?> schema) {
    Map<String, Schema<?, ?>> next = new LinkedHashMap<>(fields);
    next.put(name, schema);
    return new ObjectSchema(state, next, strict, passthrough);
  }

  /**
   * Merges another object's fields into this one. Fields from {@code other} win on a name clash.
   *
   * @param other the object whose fields to add.
   * @return a new object schema with the combined fields.
   */
  public ObjectSchema extend(ObjectSchema other) {
    Map<String, Schema<?, ?>> next = new LinkedHashMap<>(fields);
    next.putAll(other.fields);
    return new ObjectSchema(state, next, strict, passthrough);
  }

  /**
   * Keeps only the named fields and drops the rest.
   *
   * @param names the fields to keep.
   * @return a new object schema with just those fields.
   */
  public ObjectSchema pick(String... names) {
    Set<String> keep = Set.of(names);
    Map<String, Schema<?, ?>> next = new LinkedHashMap<>();
    fields.forEach((key, value) -> {
      if (keep.contains(key)) {
        next.put(key, value);
      }
    });
    return new ObjectSchema(state, next, strict, passthrough);
  }

  /**
   * Drops the named fields and keeps the rest.
   *
   * @param names the fields to remove.
   * @return a new object schema without those fields.
   */
  public ObjectSchema omit(String... names) {
    Set<String> drop = Set.of(names);
    Map<String, Schema<?, ?>> next = new LinkedHashMap<>();
    fields.forEach((key, value) -> {
      if (!drop.contains(key)) {
        next.put(key, value);
      }
    });
    return new ObjectSchema(state, next, strict, passthrough);
  }

  /**
   * Makes every field optional, so a partial object (any subset of the fields present) validates. Useful for describing update or patch payloads.
   *
   * @return a new object schema with all fields optional.
   */
  public ObjectSchema partial() {
    Map<String, Schema<?, ?>> next = new LinkedHashMap<>();
    fields.forEach((key, value) -> next.put(key, value.optional()));
    return new ObjectSchema(state, next, strict, passthrough);
  }

  /**
   * Makes every field required, the inverse of {@link #partial()}: each field's value must be present and non-null.
   *
   * @return a new object schema with all fields required.
   */
  public ObjectSchema required() {
    Map<String, Schema<?, ?>> next = new LinkedHashMap<>();
    fields.forEach((key, value) -> next.put(key, value.required()));
    return new ObjectSchema(state, next, strict, passthrough);
  }

  /**
   * Rejects unknown keys: any input key that is not a declared field produces an {@code unrecognized_key} issue.
   *
   * @return a new object schema that fails on unknown keys.
   */
  public ObjectSchema strict() {
    return new ObjectSchema(state, fields, true, false);
  }

  /**
   * Keeps unknown keys: any input key that is not a declared field is copied through to the output unchanged instead of being dropped.
   *
   * @return a new object schema that preserves unknown keys.
   */
  public ObjectSchema passthrough() {
    return new ObjectSchema(state, fields, false, true);
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
  protected Map<String, Object> jsonSchemaBody(StandardJSONSchemaV1.Options options, boolean input) {
    Map<String, Object> node = super.jsonSchemaBody(options, input);
    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();
    for (Map.Entry<String, Schema<?, ?>> entry : fields.entrySet()) {
      Schema<?, ?> field = entry.getValue();
      properties.put(entry.getKey(), input ? field.standard().jsonSchema().input(options) : field.standard().jsonSchema().output(options));
      if (!field.isOptional()) {
        required.add(entry.getKey());
      }
    }
    node.put("properties", properties);
    if (!required.isEmpty()) {
      node.put("required", required);
    }
    if (strict) {
      node.put("additionalProperties", false);
    } else if (passthrough) {
      node.put("additionalProperties", true);
    }
    return node;
  }

  @Override
  protected JsonObject process(JsonObject value, SchemaPath path, List<SchemaIssue> issues) {
    JsonObject out = new JsonObject();
    for (Map.Entry<String, Schema<?, ?>> entry : fields.entrySet()) {
      String name = entry.getKey();
      ParseResult<?> result = entry.getValue().safeParse(value.getValue(name), path.field(name));
      if (result.isSuccess()) {
        out.put(name, result.value().orElse(null));
      } else {
        issues.addAll(result.issues());
      }
    }
    for (Map.Entry<String, Object> entry : value.getMap().entrySet()) {
      if (!fields.containsKey(entry.getKey())) {
        if (strict) {
          issues.add(new SchemaIssue(path.field(entry.getKey()), "unrecognized_key", "unrecognized field"));
        } else if (passthrough) {
          out.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return out;
  }
}
