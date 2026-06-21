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

import io.vertx.core.json.JsonArray;
import io.vertx.schema.*;
import io.vertx.schema.standard.v1.StandardJSONSchemaV1;
import java.util.List;
import java.util.Map;

/**
 * A schema for lists. It checks that the input is a {@link JsonArray}, validates every element against the element schema, and reports a problem in element <i>i</i> at the path
 * {@code "[i]"} (composing with any outer path, so a bad element inside an object field reads like {@code "tags[2]"}). The size refinements ({@link #min}, {@link #max},
 * {@link #nonEmpty}) constrain the list itself rather than its elements.
 *
 * <p>Validation produces a new array of the validated elements, and the input array is not modified.
 *
 * @param <E> the element type.
 */
public final class ArraySchema<E> extends Schema<JsonArray, ArraySchema<E>> {

  private final Schema<E, ?> element;

  ArraySchema(State<JsonArray> state, Schema<E, ?> element) {
    super(state);
    this.element = element;
  }

  @SuppressWarnings("unchecked")
  static <E> ArraySchema<E> of(Schema<E, ?> element) {
    FieldType<JsonArray> type = (FieldType<JsonArray>) (FieldType<?>) FieldType.ARRAY;
    return new ArraySchema<>(State.of(type), element);
  }

  @Override
  protected ArraySchema<E> withState(State<JsonArray> state) {
    return new ArraySchema<>(state, element);
  }

  /**
   * The schema each element is validated against.
   *
   * @return the element schema.
   */
  public Schema<E, ?> element() {
    return element;
  }

  /**
   * Requires the list to hold at least {@code min} elements.
   *
   * @param min the minimum size, inclusive.
   * @return a new schema carrying the rule.
   */
  public ArraySchema<E> min(int min) {
    return check("too_small", list -> list.size() >= min, "must have at least " + min + " items", Map.of("minItems", min));
  }

  /**
   * Requires the list to hold at most {@code max} elements.
   *
   * @param max the maximum size, inclusive.
   * @return a new schema carrying the rule.
   */
  public ArraySchema<E> max(int max) {
    return check("too_big", list -> list.size() <= max, "must have at most " + max + " items", Map.of("maxItems", max));
  }

  /**
   * Requires the list to hold exactly {@code length} elements.
   *
   * @param length the required size.
   * @return a new schema carrying the rule.
   */
  public ArraySchema<E> length(int length) {
    return check("invalid_length", list -> list.size() == length, "must have exactly " + length + " items", Map.of("minItems", length, "maxItems", length));
  }

  /**
   * Requires the list to hold at least one element.
   *
   * @return a new schema carrying the rule.
   */
  public ArraySchema<E> nonEmpty() {
    return min(1);
  }

  @Override
  protected JsonArray coerce(Object input) {
    if (input instanceof JsonArray) {
      return (JsonArray) input;
    }
    if (input instanceof List) {
      return new JsonArray((List<?>) input);
    }
    throw new ClassCastException();
  }

  @Override
  protected Map<String, Object> jsonSchemaBody(StandardJSONSchemaV1.Options options, boolean input) {
    Map<String, Object> node = super.jsonSchemaBody(options, input);
    node.put("items", input ? element.standard().jsonSchema().input(options) : element.standard().jsonSchema().output(options));
    return node;
  }

  @Override
  protected JsonArray process(JsonArray value, SchemaPath path, List<SchemaIssue> issues) {
    JsonArray out = new JsonArray();
    for (int i = 0; i < value.size(); i++) {
      ParseResult<E> result = element.safeParse(value.getValue(i), path.index(i));
      if (result.isSuccess()) {
        out.add(result.value().orElse(null));
      } else {
        issues.addAll(result.issues());
      }
    }
    return out;
  }
}
