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

import java.util.Map;
import java.util.Objects;

/**
 * The JSON Schema conversion contract of the <a href="https://standardschema.dev/json-schema">Standard Schema</a> family, version 1. It is orthogonal to {@link StandardSchemaV1}.
 * It carries no validation, only a {@link Converter} that renders the schema as a JSON Schema document. Its {@link Props} extend the {@link StandardTypedV1.Props typed base} with
 * {@link Props#jsonSchema()}, so a schema may implement both this and {@link StandardSchemaV1}.
 *
 * <p>Conversion often differs for the value going in versus the value coming out (a schema may coerce {@code "123"} into
 * {@code 123}), so the converter exposes separate {@link Converter#input(Options)} and {@link Converter#output(Options)} methods. The JSON Schema document is modelled as a
 * {@code Map<String, Object>}, the spec's {@code Record<string, unknown>}. A caller serializes it with whatever JSON library it prefers.
 *
 * @param <I> the accepted input type.
 * @param <O> the produced output type.
 */
public interface StandardJSONSchemaV1<I, O> extends StandardTypedV1<I, O> {

  @Override
  Props<I, O> standard();

  /**
   * The JSON Schema conversion properties: the typed base plus {@link #jsonSchema()}.
   *
   * @param <I> the accepted input type.
   * @param <O> the produced output type.
   */
  interface Props<I, O> extends StandardTypedV1.Props<I, O> {

    /**
     * The JSON Schema converter for this schema.
     *
     * @return the converter.
     */
    Converter jsonSchema();
  }

  interface Converter {

    /**
     * The JSON Schema for the values this schema accepts.
     *
     * @param options the conversion options.
     * @return the JSON Schema document.
     */
    Map<String, Object> input(Options options);

    /**
     * The JSON Schema for the values this schema produces.
     *
     * @param options the conversion options.
     * @return the JSON Schema document.
     */
    Map<String, Object> output(Options options);
  }

  /**
   * Conversion options.
   *
   * @param target the JSON Schema dialect to target.
   * @param libraryOptions implementation-specific options, never {@code null} (use an empty map for none).
   */
  final class Options {

    private final Target target;
    private final Map<String, Object> libraryOptions;

    public Options(Target target, Map<String, Object> libraryOptions) {
      this.target = target;
      this.libraryOptions = libraryOptions;
    }

    /**
     * Options for {@code target} with no library-specific options.
     *
     * @param target the dialect to target.
     * @return the options.
     */
    public static Options of(Target target) {
      return new Options(target, Map.of());
    }

    public Target target() {
      return target;
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
      return Objects.equals(target, options.target) && Objects.equals(libraryOptions, options.libraryOptions);
    }

    @Override
    public int hashCode() {
      return Objects.hash(target, libraryOptions);
    }

    @Override
    public String toString() {
      return "Options[target=" + target + ", libraryOptions=" + libraryOptions + "]";
    }
  }

  /**
   * A JSON Schema dialect. The known dialects are provided as constants, but the spec leaves the set open, so any identifier is accepted.
   *
   * @param id the dialect identifier.
   */
  final class Target {

    public static final Target DRAFT_2020_12 = new Target("draft-2020-12");
    public static final Target DRAFT_07 = new Target("draft-07");
    public static final Target OPENAPI_3_0 = new Target("openapi-3.0");

    private final String id;

    public Target(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Target)) {
        return false;
      }
      Target target = (Target) o;
      return Objects.equals(id, target.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "Target[id=" + id + "]";
    }
  }
}
