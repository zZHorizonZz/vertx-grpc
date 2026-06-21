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

import io.vertx.schema.standard.v1.StandardSchemaV1;
import io.vertx.schema.standard.v1.StandardTypedV1;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A schema that validates with a source {@link Schema} and then maps the validated value to a different output type. It is the decorator form of zod's {@code transform}, created
 * by {@link Schema#transform(Function)}.
 *
 * <p>It is deliberately a thin wrapper rather than a {@link Schema}. The base {@code Schema<T, SELF>} carries one type for
 * both input and output, so a transform (where they differ) lives beside it instead of widening it. As a result a transform is terminal. It conforms to {@link StandardSchemaV1}
 * and offers {@link #parse} and {@link #safeParse}, but cannot be further refined or nested as an object field. It does not implement {@code StandardJSONSchemaV1}, since the
 * output type produced by an arbitrary mapper cannot be introspected into JSON Schema.
 *
 * @param <In> the validated input type.
 * @param <Out> the mapped output type.
 */
public final class TransformSchema<In, Out> implements StandardSchemaV1<In, Out> {

  private final Schema<In, ?> source;
  private final Function<? super In, ? extends Out> mapper;

  TransformSchema(Schema<In, ?> source, Function<? super In, ? extends Out> mapper) {
    this.source = source;
    this.mapper = mapper;
  }

  /**
   * The schema whose validation feeds this transform.
   *
   * @return the source schema.
   */
  public Schema<In, ?> source() {
    return source;
  }

  /**
   * Validates {@code input} with the source schema and, on success, maps the value through the transform.
   *
   * @param input the value to validate.
   * @return a success holding the mapped value, or a failure holding the source's issues.
   */
  public ParseResult<Out> safeParse(Object input) {
    ParseResult<In> result = source.safeParse(input);
    if (!result.isSuccess()) {
      return ParseResult.failure(result.issues());
    }
    return ParseResult.success(mapper.apply(result.value().orElse(null)));
  }

  /**
   * Validates and maps {@code input}, throwing on failure.
   *
   * @param input the value to validate.
   * @return the mapped value.
   * @throws SchemaError if validation fails.
   */
  public Out parse(Object input) {
    return safeParse(input).orElseThrow();
  }

  @Override
  public Props<In, Out> standard() {
    return new TransformProps();
  }

  private final class TransformProps implements StandardSchemaV1.Props<In, Out> {

    @Override
    public int version() {
      return 1;
    }

    @Override
    public String vendor() {
      return source.standard().vendor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public StandardTypedV1.Types<In, Out> types() {
      Class<In> in = source.standard().types().input();
      return new StandardTypedV1.Types<>(in, (Class<Out>) Object.class);
    }

    @Override
    public CompletableFuture<Result<Out>> validate(Object value, Options options) {
      return CompletableFuture.completedFuture(Schema.toStandardResult(safeParse(value)));
    }
  }
}
