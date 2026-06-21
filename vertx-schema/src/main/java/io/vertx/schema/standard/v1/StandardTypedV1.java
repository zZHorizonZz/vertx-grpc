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

import java.util.Objects;

/**
 * The shared base of the <a href="https://standardschema.dev">Standard Schema</a> family, version 1. It carries only the metadata common to every contract in the family. That is
 * the version, the producing vendor, and the input/output type tokens, with no capability of its own. {@link StandardSchemaV1} adds validation, and {@link StandardJSONSchemaV1}
 * adds JSON Schema conversion. A single schema can implement several of these at once, and each exposes its properties through {@link #standard()}.
 *
 * <p>This is a Java port, not the literal TypeScript spec. The spec hangs everything off a structural {@code "~standard"}
 * property. Java is nominal, so that property becomes the {@link #standard()} accessor and the {@code Props} types form a real interface hierarchy, where
 * {@link StandardSchemaV1.Props} and {@link StandardJSONSchemaV1.Props} both extend {@link Props}. The version is reported both by the type name ({@code StandardTypedV1}) and by
 * {@link Props#version()}, exactly as the spec does it.
 *
 * @param <I> the accepted input type.
 * @param <O> the produced output type (equal to {@code I} for a schema that validates rather than transforms).
 */
public interface StandardTypedV1<I, O> {

  /**
   * The Standard Schema properties for this schema. This is the Java stand-in for the spec's {@code "~standard"} property.
   *
   * @return the properties.
   */
  Props<I, O> standard();

  /**
   * The metadata common to every Standard Schema contract. Capability contracts extend this with their own properties.
   *
   * @param <I> the accepted input type.
   * @param <O> the produced output type.
   */
  interface Props<I, O> {

    /**
     * The Standard Schema contract version, always {@code 1} for this family.
     *
     * @return {@code 1}.
     */
    int version();

    /**
     * The name of the library that produced this schema.
     *
     * @return the vendor identifier.
     */
    String vendor();

    /**
     * The runtime input/output type tokens. The spec's {@code types} field is type-only and erased. Carrying real {@link Class} tokens is the faithful analog, and in Java it is
     * more useful.
     *
     * @return the type tokens.
     */
    Types<I, O> types();
  }

  /**
   * The input and output type tokens of a schema.
   *
   * @param input the accepted input type token.
   * @param output the produced output type token.
   * @param <I> the accepted input type.
   * @param <O> the produced output type.
   */
  final class Types<I, O> {

    private final Class<I> input;
    private final Class<O> output;

    public Types(Class<I> input, Class<O> output) {
      this.input = input;
      this.output = output;
    }

    public Class<I> input() {
      return input;
    }

    public Class<O> output() {
      return output;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Types)) {
        return false;
      }
      Types<?, ?> types = (Types<?, ?>) o;
      return Objects.equals(input, types.input) && Objects.equals(output, types.output);
    }

    @Override
    public int hashCode() {
      return Objects.hash(input, output);
    }

    @Override
    public String toString() {
      return "Types[input=" + input + ", output=" + output + "]";
    }
  }
}
