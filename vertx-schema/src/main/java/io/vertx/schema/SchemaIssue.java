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

import java.util.Objects;

/**
 * A single validation problem found while parsing a value against a {@link Schema}.
 *
 * @param path the location of the problem, {@link SchemaPath#ROOT} for the root or a nested path such as {@code ["address", "zip"]}. Use {@link #pathString()} for the flat
 * {@code "address.zip"} form.
 * @param code a stable machine-readable code (for example {@code "required"}, {@code "invalid_type"}, {@code "too_small"}, {@code "custom"}). This is richer than the Standard
 * Schema contract, which carries only the message and path.
 * @param message a human-readable description.
 */
public final class SchemaIssue {

  private final SchemaPath path;
  private final String code;
  private final String message;

  public SchemaIssue(SchemaPath path, String code, String message) {
    this.path = path;
    this.code = code;
    this.message = message;
  }

  public SchemaPath path() {
    return path;
  }

  public String code() {
    return code;
  }

  public String message() {
    return message;
  }

  public String pathString() {
    return path.render();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SchemaIssue)) {
      return false;
    }
    SchemaIssue that = (SchemaIssue) o;
    return Objects.equals(path, that.path) && Objects.equals(code, that.code) && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, code, message);
  }

  @Override
  public String toString() {
    return (path.isRoot() ? "<root>" : path.render()) + ": " + message + " [" + code + "]";
  }
}
