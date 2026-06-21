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
package io.vertx.schema.protobuf;

import io.vertx.schema.MetaKey;
import io.vertx.schema.Schema;

/**
 * The Protobuf target's vocabulary over a general {@code io.vertx.schema.Schema}: the metadata
 * keys it reads, and the {@link #field(int, Schema)} helper for attaching a field number without
 * the {@code .meta(...)} ceremony.
 * <p>
 * The schema itself stays backend-agnostic; protobuf-specific information (most importantly
 * the field number) is attached through the schema's metadata channel and read back here -
 * the equivalent of TypeSpec's {@code @field(n)} or protobuf-net's {@code [ProtoMember(n)]}.
 *
 * <pre>{@code
 * Schemas.object()
 *   .field("id",    Proto.field(1, Schemas.uuid()))
 *   .field("email", Proto.field(2, Schemas.string()));
 * }</pre>
 */
public final class Proto {

  private Proto() {
  }

  /**
   * The Protobuf field number of a field (wire tag). Required on every field of a message
   * that is encoded to the protobuf wire format.
   */
  public static final MetaKey<Integer> FIELD = MetaKey.of("pb.number");

  // TODO: ENCODING (varint vs zig-zag vs fixed, for the sint*/fixed* variants),
  //       PACKAGE (namespace -> .proto package), RESERVE (retired numbers/names).

  /**
   * Tag a schema with its Protobuf field {@code number}, returning the same (concrete) schema type
   * so it can be dropped straight into {@code Schemas.object().field(name, Proto.field(n, schema))}.
   * Equivalent to {@code schema.meta(Proto.FIELD, number)} but terser and harder to forget.
   *
   * @param number the protobuf field number (wire tag)
   * @param schema the field's schema
   * @param <S>    the concrete schema type, preserved through the call
   * @return the schema carrying the field number
   */
  public static <S extends Schema<?, S>> S field(int number, S schema) {
    return schema.meta(FIELD, number);
  }
}
