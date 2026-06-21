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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of {@link Schema#safeParse(Object)}: either a {@link Success} carrying the validated value, or a {@link Failure} carrying the {@link SchemaIssue}s. Lets callers
 * handle invalid input without exceptions.
 *
 * @param <T> the validated value type.
 */
public interface ParseResult<T> {

  static <T> ParseResult<T> success(T value) {
    return new Success<>(value);
  }

  static <T> ParseResult<T> failure(List<SchemaIssue> issues) {
    return new Failure<>(List.copyOf(issues));
  }

  static <T> ParseResult<T> failure(SchemaIssue issue) {
    return new Failure<>(List.of(issue));
  }

  boolean isSuccess();

  Optional<T> value();

  List<SchemaIssue> issues();

  /**
   * Returns the validated value, or throws {@link SchemaError} with every issue on failure.
   *
   * @return the validated value (may be {@code null} when the schema is optional).
   */
  T orElseThrow();

  final class Success<T> implements ParseResult<T> {

    private final T validated;

    public Success(T validated) {
      this.validated = validated;
    }

    public T validated() {
      return validated;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public Optional<T> value() {
      return Optional.ofNullable(validated);
    }

    @Override
    public List<SchemaIssue> issues() {
      return List.of();
    }

    @Override
    public T orElseThrow() {
      return validated;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Success)) {
        return false;
      }
      Success<?> success = (Success<?>) o;
      return Objects.equals(validated, success.validated);
    }

    @Override
    public int hashCode() {
      return Objects.hash(validated);
    }

    @Override
    public String toString() {
      return "Success[validated=" + validated + "]";
    }
  }

  final class Failure<T> implements ParseResult<T> {

    private final List<SchemaIssue> issues;

    public Failure(List<SchemaIssue> issues) {
      this.issues = issues;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public Optional<T> value() {
      return Optional.empty();
    }

    @Override
    public List<SchemaIssue> issues() {
      return issues;
    }

    @Override
    public T orElseThrow() {
      throw new SchemaError(issues);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Failure)) {
        return false;
      }
      Failure<?> failure = (Failure<?>) o;
      return Objects.equals(issues, failure.issues);
    }

    @Override
    public int hashCode() {
      return Objects.hash(issues);
    }

    @Override
    public String toString() {
      return "Failure[issues=" + issues + "]";
    }
  }
}
