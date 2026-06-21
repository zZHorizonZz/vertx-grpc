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
package io.vertx.schema.standard.v1;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The validation contract of the <a href="https://standardschema.dev">Standard Schema</a> family, version 1. It is a library-agnostic entry point that a generic consumer such as a
 * form library or an RPC layer can call without depending on the schema's own types. Its {@link Props} extend the {@link StandardTypedV1.Props typed base} with
 * {@link Props#validate}.
 *
 * <p>The contract is asynchronous, since {@link Props#validate} returns a {@link CompletableFuture}, so a consumer
 * always handles a possibly-deferred result. A synchronous implementation simply returns an already-completed future.
 *
 * @param <I> the accepted input type.
 * @param <O> the validated output type.
 */
public interface StandardSchemaV1<I, O> extends StandardTypedV1<I, O> {

  @Override
  Props<I, O> standard();

  /**
   * The validation properties: the typed base plus {@link #validate}.
   *
   * @param <I> the accepted input type.
   * @param <O> the validated output type.
   */
  interface Props<I, O> extends StandardTypedV1.Props<I, O> {

    /**
     * Validates {@code value}.
     *
     * @param value the value to validate.
     * @param options vendor-specific options, never {@code null}. Use {@link Options#none()} for none.
     * @return a future of the success-or-issues result.
     */
    CompletableFuture<Result<O>> validate(Object value, Options options);
  }

  /**
   * The outcome of a validation: a {@link Success} carrying the output, or a {@link Failure} carrying the issues.
   *
   * @param <O> the validated output type.
   */
  interface Result<O> {

    static <O> Result<O> success(O value) {
      return new Success<>(value);
    }

    static <O> Result<O> failure(List<Issue> issues) {
      return new Failure<>(List.copyOf(issues));
    }
  }

  /**
   * Validation options.
   *
   * @param libraryOptions implementation-specific options, never {@code null}.
   */
  final class Options {

    private static final Options NONE = new Options(Map.of());

    private final Map<String, Object> libraryOptions;

    public Options(Map<String, Object> libraryOptions) {
      this.libraryOptions = libraryOptions;
    }

    /**
     * Options with no library-specific parameters.
     *
     * @return the empty options.
     */
    public static Options none() {
      return NONE;
    }

    public Map<String, Object> libraryOptions() {
      return libraryOptions;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Options)) {
        return false;
      }
      Options options = (Options) o;
      return Objects.equals(libraryOptions, options.libraryOptions);
    }

    @Override
    public int hashCode() {
      return Objects.hash(libraryOptions);
    }

    @Override
    public String toString() {
      return "Options[libraryOptions=" + libraryOptions + "]";
    }
  }

  /**
   * A successful validation.
   *
   * @param value the validated value.
   * @param <O> the validated output type.
   */
  final class Success<O> implements Result<O> {

    private final O value;

    public Success(O value) {
      this.value = value;
    }

    public O value() {
      return value;
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
      return Objects.equals(value, success.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Success[value=" + value + "]";
    }
  }

  /**
   * A failed validation.
   *
   * @param issues the problems found, never empty.
   * @param <O> the validated output type.
   */
  final class Failure<O> implements Result<O> {

    private final List<Issue> issues;

    public Failure(List<Issue> issues) {
      this.issues = issues;
    }

    public List<Issue> issues() {
      return issues;
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

  /**
   * A single problem in the spec's shape, with a message and a structured path. An implementation's own issue type may carry more, for example a machine-readable code. This
   * contract exposes only what every consumer can rely on.
   *
   * @param message a human-readable description.
   * @param path the location of the problem as a list of segments.
   */
  final class Issue {

    private final String message;
    private final List<PathSegment> path;

    public Issue(String message, List<PathSegment> path) {
      this.message = message;
      this.path = path;
    }

    public String message() {
      return message;
    }

    public List<PathSegment> path() {
      return path;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Issue)) {
        return false;
      }
      Issue issue = (Issue) o;
      return Objects.equals(message, issue.message) && Objects.equals(path, issue.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(message, path);
    }

    @Override
    public String toString() {
      return "Issue[message=" + message + ", path=" + path + "]";
    }
  }

  /**
   * One step in an {@link Issue} path.
   *
   * @param key the segment key: a {@link String} field name or an {@link Integer} array index.
   */
  final class PathSegment {

    private final Object key;

    public PathSegment(Object key) {
      this.key = key;
    }

    public Object key() {
      return key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PathSegment)) {
        return false;
      }
      PathSegment that = (PathSegment) o;
      return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key);
    }

    @Override
    public String toString() {
      return "PathSegment[key=" + key + "]";
    }
  }
}
