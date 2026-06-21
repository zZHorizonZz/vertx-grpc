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

import io.vertx.schema.FieldType;
import io.vertx.schema.Schema;
import java.time.Instant;

/**
 * A schema for {@link Instant} timestamps. On top of the universal modifiers it adds inclusive lower and upper bounds on the moment in time. It renders to JSON Schema as a
 * {@code string} with {@code date-time} format. The bounds have no portable JSON Schema keyword and so are validation-only.
 */
public final class DateSchema extends Schema<Instant, DateSchema> {

  DateSchema(State<Instant> state) {
    super(state);
  }

  static DateSchema create() {
    return new DateSchema(State.of(FieldType.TIMESTAMP));
  }

  @Override
  protected DateSchema withState(State<Instant> state) {
    return new DateSchema(state);
  }

  /**
   * Requires the timestamp to be at or after {@code bound}. Earlier instants fail with a {@code too_small} issue.
   *
   * @param bound the earliest accepted instant, inclusive.
   * @return a new schema carrying the rule.
   */
  public DateSchema min(Instant bound) {
    return check("too_small", value -> !value.isBefore(bound), "must be on or after " + bound);
  }

  /**
   * Requires the timestamp to be at or before {@code bound}. Later instants fail with a {@code too_big} issue.
   *
   * @param bound the latest accepted instant, inclusive.
   * @return a new schema carrying the rule.
   */
  public DateSchema max(Instant bound) {
    return check("too_big", value -> !value.isAfter(bound), "must be on or before " + bound);
  }
}
