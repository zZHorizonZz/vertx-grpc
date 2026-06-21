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

import java.util.ArrayList;
import java.util.List;

/**
 * The location of a value inside a composite, held as an ordered list of segments rather than a pre-rendered string. A segment is either a {@link String} (an object field name) or
 * an {@link Integer} (an array index). Keeping the structure means {@link Schema} can report a problem at, for example, {@code ["address", "zip"]} and a caller can both render the
 * familiar dotted form ({@link #render()}) and walk the segments programmatically. Walking the segments is what the Standard Schema contract exposes.
 *
 * <p>Instances are immutable. {@link #field(String)} and {@link #index(int)} return a new path with one more segment.
 */
public final class SchemaPath {

  public static final SchemaPath ROOT = new SchemaPath(List.of());

  private final List<Object> segments;

  private SchemaPath(List<Object> segments) {
    this.segments = segments;
  }

  /**
   * Returns this path extended by an object field name.
   *
   * @param name the field name to append.
   * @return a new path ending in {@code name}.
   */
  public SchemaPath field(String name) {
    return append(name);
  }

  /**
   * Returns this path extended by an array index.
   *
   * @param index the element index to append.
   * @return a new path ending in {@code index}.
   */
  public SchemaPath index(int index) {
    return append(index);
  }

  private SchemaPath append(Object segment) {
    List<Object> next = new ArrayList<>(segments.size() + 1);
    next.addAll(segments);
    next.add(segment);
    return new SchemaPath(List.copyOf(next));
  }

  public boolean isRoot() {
    return segments.isEmpty();
  }

  public List<Object> segments() {
    return segments;
  }

  /**
   * Renders the path in the conventional flat form: field names joined by dots and array indices in brackets, so {@code ["tags", 2]} reads as {@code "tags[2]"} and
   * {@code ["address", "zip"]} as {@code "address.zip"}. The root renders as the empty string.
   *
   * @return the flattened string form.
   */
  public String render() {
    StringBuilder sb = new StringBuilder();
    for (Object segment : segments) {
      if (segment instanceof Integer) {
        sb.append('[').append(segment).append(']');
      } else if (sb.length() == 0) {
        sb.append(segment);
      } else {
        sb.append('.').append(segment);
      }
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return render();
  }
}
