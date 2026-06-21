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
import java.util.Map;

/**
 * A schema for numeric values, shared across every numeric width ({@code int32}, {@code int64}, {@code float32}, {@code float64}). The carrier {@code N} is the concrete number
 * type the corresponding {@link Schemas} factory chose. On top of the universal modifiers it adds bound refinements that compare against the value using its natural ordering.
 *
 * @param <N> the numeric carrier, for example {@code Integer}, {@code Long}, {@code Float} or {@code Double}.
 */
public final class NumberSchema<N extends Number & Comparable<N>> extends Schema<N, NumberSchema<N>> {

  NumberSchema(State<N> state) {
    super(state);
  }

  static <N extends Number & Comparable<N>> NumberSchema<N> create(FieldType<N> type) {
    return new NumberSchema<>(State.of(type));
  }

  @Override
  protected NumberSchema<N> withState(State<N> state) {
    return new NumberSchema<>(state);
  }

  /**
   * Sets an inclusive lower bound. Values below {@code bound} fail with a {@code too_small} issue, and {@code bound} itself is accepted.
   *
   * @param bound the smallest accepted value.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> min(N bound) {
    return check("too_small", value -> value.compareTo(bound) >= 0, "must be at least " + bound, Map.of("minimum", bound));
  }

  /**
   * Sets an inclusive upper bound. Values above {@code bound} fail with a {@code too_big} issue, and {@code bound} itself is accepted.
   *
   * @param bound the largest accepted value.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> max(N bound) {
    return check("too_big", value -> value.compareTo(bound) <= 0, "must be at most " + bound, Map.of("maximum", bound));
  }

  /**
   * Requires the value to be strictly greater than {@code bound}, so {@code bound} itself is rejected. Failures use the {@code too_small} code.
   *
   * @param bound the exclusive lower bound.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> gt(N bound) {
    return check("too_small", value -> value.compareTo(bound) > 0, "must be greater than " + bound, Map.of("exclusiveMinimum", bound));
  }

  /**
   * Requires the value to be strictly less than {@code bound}, so {@code bound} itself is rejected. Failures use the {@code too_big} code.
   *
   * @param bound the exclusive upper bound.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> lt(N bound) {
    return check("too_big", value -> value.compareTo(bound) < 0, "must be less than " + bound, Map.of("exclusiveMaximum", bound));
  }

  /**
   * An inclusive lower bound, an alias for {@link #min(Number)} matching the {@code >=} reading.
   *
   * @param bound the smallest accepted value.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> gte(N bound) {
    return min(bound);
  }

  /**
   * An inclusive upper bound, an alias for {@link #max(Number)} matching the {@code <=} reading.
   *
   * @param bound the largest accepted value.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> lte(N bound) {
    return max(bound);
  }

  /**
   * Requires the value to be strictly greater than zero.
   *
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> positive() {
    return check("too_small", value -> value.doubleValue() > 0, "must be positive", Map.of("exclusiveMinimum", 0));
  }

  /**
   * Requires the value to be zero or greater.
   *
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> nonnegative() {
    return check("too_small", value -> value.doubleValue() >= 0, "must be non-negative", Map.of("minimum", 0));
  }

  /**
   * Requires the value to be strictly less than zero.
   *
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> negative() {
    return check("too_big", value -> value.doubleValue() < 0, "must be negative", Map.of("exclusiveMaximum", 0));
  }

  /**
   * Requires the value to be zero or less.
   *
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> nonpositive() {
    return check("too_big", value -> value.doubleValue() <= 0, "must be non-positive", Map.of("maximum", 0));
  }

  /**
   * Requires the value to be an exact multiple of {@code divisor}.
   *
   * @param divisor the divisor the value must be a multiple of.
   * @return a new schema carrying the rule.
   */
  public NumberSchema<N> multipleOf(N divisor) {
    return check("not_multiple_of", value -> value.doubleValue() % divisor.doubleValue() == 0, "must be a multiple of " + divisor, Map.of("multipleOf", divisor));
  }
}
