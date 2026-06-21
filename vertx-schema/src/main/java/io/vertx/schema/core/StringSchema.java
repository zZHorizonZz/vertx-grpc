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
import io.vertx.schema.SchemaIssue;
import io.vertx.schema.SchemaPath;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A schema for {@code String} values. On top of the universal modifiers it adds the common string refinements (length bounds, substring matchers, pattern matching via
 * {@link #matches(StringFormat)} with a custom expression or a named {@link StringFormat} preset such as {@code format().email()}, and a closed set of allowed values). It also
 * adds value-preserving mutators ({@link #trim()}, {@link #toLowerCase()}, {@link #toUpperCase()}) that normalize the string before any refinement runs. Each refinement runs only
 * after the input has been confirmed to be a string.
 */
public final class StringSchema extends Schema<String, StringSchema> {

  private final Function<String, String> preprocess;

  StringSchema(State<String> state) {
    this(state, Function.identity());
  }

  StringSchema(State<String> state, Function<String, String> preprocess) {
    super(state);
    this.preprocess = preprocess;
  }

  static StringSchema create() {
    return new StringSchema(State.of(FieldType.STRING));
  }

  private static String quote(String literal) {
    StringBuilder sb = new StringBuilder(literal.length());
    for (int i = 0; i < literal.length(); i++) {
      char c = literal.charAt(i);
      if ("\\^$.|?*+()[]{}".indexOf(c) >= 0) {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  @Override
  protected StringSchema withState(State<String> state) {
    return new StringSchema(state, preprocess);
  }

  private StringSchema withPreprocess(Function<String, String> next) {
    return new StringSchema(state, preprocess.andThen(next));
  }

  /**
   * Requires at least {@code min} characters. Shorter strings fail with a {@code too_small} issue.
   *
   * @param min the minimum length, inclusive.
   * @return a new schema carrying the rule.
   */
  public StringSchema min(int min) {
    return check("too_small", value -> value.length() >= min, "must be at least " + min + " characters", Map.of("minLength", min));
  }

  /**
   * Requires at most {@code max} characters. Longer strings fail with a {@code too_big} issue.
   *
   * @param max the maximum length, inclusive.
   * @return a new schema carrying the rule.
   */
  public StringSchema max(int max) {
    return check("too_big", value -> value.length() <= max, "must be at most " + max + " characters", Map.of("maxLength", max));
  }

  /**
   * Requires exactly {@code length} characters.
   *
   * @param length the required length.
   * @return a new schema carrying the rule.
   */
  public StringSchema length(int length) {
    return check("invalid_length", value -> value.length() == length, "must be exactly " + length + " characters", Map.of("minLength", length, "maxLength", length));
  }

  /**
   * Requires the whole string to match {@code format} (a custom expression or a named {@link StringFormat} preset). The pattern is anchored at both ends, so it must match the
   * entire value, not just a part of it. It contributes its JSON Schema keywords (a {@code format} for presets, a {@code pattern} for custom expressions).
   *
   * @param format the format the value must match.
   * @return a new schema carrying the rule.
   */
  public StringSchema matches(StringFormat format) {
    return check("invalid_string", value -> format.pattern().matcher(value).matches(), "must match " + format.description(), format.jsonSchema());
  }

  /**
   * Requires the whole string to match the custom expression {@code regex}, shorthand for {@code matches(StringFormat.format(regex))}.
   *
   * @param regex the regular expression the value must match.
   * @return a new schema carrying the rule.
   */
  public StringSchema regex(String regex) {
    return matches(StringFormat.format(regex));
  }

  /**
   * Requires the string to look like an email address, shorthand for {@code matches(format().email())}.
   *
   * @return a new schema carrying the rule.
   */
  public StringSchema email() {
    return matches(StringFormat.format().email());
  }

  /**
   * Requires the string to start with {@code prefix}.
   *
   * @param prefix the required leading text.
   * @return a new schema carrying the rule.
   */
  public StringSchema startsWith(String prefix) {
    return check("invalid_string", value -> value.startsWith(prefix), "must start with " + prefix, Map.of("pattern", "^" + quote(prefix)));
  }

  /**
   * Requires the string to end with {@code suffix}.
   *
   * @param suffix the required trailing text.
   * @return a new schema carrying the rule.
   */
  public StringSchema endsWith(String suffix) {
    return check("invalid_string", value -> value.endsWith(suffix), "must end with " + suffix, Map.of("pattern", quote(suffix) + "$"));
  }

  /**
   * Requires the string to contain {@code substring}.
   *
   * @param substring the required text.
   * @return a new schema carrying the rule.
   */
  public StringSchema includes(String substring) {
    return check("invalid_string", value -> value.contains(substring), "must include " + substring, Map.of("pattern", quote(substring)));
  }

  /**
   * Restricts the value to one of {@code allowed} (a string enumeration).
   *
   * @param allowed the permitted values.
   * @return a new schema carrying the rule.
   */
  public StringSchema oneOf(String... allowed) {
    Set<String> set = Set.of(allowed);
    return check("invalid_enum_value", set::contains, "must be one of " + set, Map.of("enum", List.of(allowed)));
  }

  /**
   * Strips leading and trailing whitespace from the value before any refinement runs (and so before it is returned).
   *
   * @return a new schema carrying the mutator.
   */
  public StringSchema trim() {
    return withPreprocess(String::trim);
  }

  /**
   * Lower-cases the value (in the root locale) before any refinement runs.
   *
   * @return a new schema carrying the mutator.
   */
  public StringSchema toLowerCase() {
    return withPreprocess(value -> value.toLowerCase(Locale.ROOT));
  }

  /**
   * Upper-cases the value (in the root locale) before any refinement runs.
   *
   * @return a new schema carrying the mutator.
   */
  public StringSchema toUpperCase() {
    return withPreprocess(value -> value.toUpperCase(Locale.ROOT));
  }

  @Override
  protected String process(String value, SchemaPath path, List<SchemaIssue> issues) {
    return preprocess.apply(value);
  }
}
