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

import io.vertx.schema.standard.v1.StandardJSONSchemaV1;
import io.vertx.schema.standard.v1.StandardSchemaV1;
import io.vertx.schema.standard.v1.StandardTypedV1;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An immutable description of a single value's shape: its {@link FieldType}, whether it may be {@code null}, an optional default, the list of {@link Check refinements} applied
 * during validation, and an open {@link MetaKey metadata channel} that targets read from. Validation happens through {@link #parse(Object)} (throwing) or
 * {@link #safeParse(Object)} (returning a result).
 *
 * <p>The class is parameterized by its own subtype ({@code SELF}, the self-type / CRTP pattern, as in AssertJ's
 * {@code AbstractAssert}). That lets a fluent method declared here, such as {@link #optional()}, return the concrete subtype rather than {@code Schema}, so a chain like
 * {@code Schemas.string().optional().min(3)} stays a {@code StringSchema} the whole way. Concrete schemas (such as {@code StringSchema}, {@code NumberSchema} and
 * {@code ObjectSchema}) extend this class and add their own refinement methods. A composite schema additionally overrides {@link #coerce(Object)} and
 * {@link #process(Object, String, List)} to validate the values it contains.
 *
 * <p>Instances are immutable: every fluent method returns a new schema and leaves the receiver untouched, so a schema is
 * safe to build once and reuse across threads.
 *
 * @param <T> the Java value type this schema validates.
 * @param <SELF> the concrete subtype, returned by every fluent method.
 */
public abstract class Schema<T, SELF extends Schema<T, SELF>> implements StandardSchemaV1<T, T>, StandardJSONSchemaV1<T, T> {

  private static final String VENDOR = "vertx";

  protected final State<T> state;

  /**
   * @param state the universal state this schema wraps.
   */
  protected Schema(State<T> state) {
    this.state = state;
  }

  static <T> StandardSchemaV1.Result<T> toStandardResult(ParseResult<T> result) {
    if (result.isSuccess()) {
      return StandardSchemaV1.Result.success(result.value().orElse(null));
    }
    List<StandardSchemaV1.Issue> issues = result.issues().stream()
      .map(issue -> new StandardSchemaV1.Issue(
        issue.message(),
        issue.path().segments().stream().map(StandardSchemaV1.PathSegment::new).collect(Collectors.toList()))
      ).collect(Collectors.toList());
    return StandardSchemaV1.Result.failure(issues);
  }

  private static void applyNullable(Map<String, Object> node, StandardJSONSchemaV1.Options options) {
    if (StandardJSONSchemaV1.Target.OPENAPI_3_0.equals(options.target())) {
      node.put("nullable", true);
    } else {
      Object type = node.get("type");
      if (type != null) {
        node.put("type", List.of(type, "null"));
      }
    }
  }

  /**
   * Produces a new instance of the concrete subtype around {@code state}, preserving any extra fields the subtype holds (for example an {@code ArraySchema}'s element schema). This
   * is the one method a subtype must implement, and it is what lets the fluent methods here return {@code SELF}.
   *
   * @param state the state the new instance should carry.
   * @return a new instance of the subtype.
   */
  protected abstract SELF withState(State<T> state);

  /**
   * The value type this schema validates against, for example {@link FieldType#STRING}.
   *
   * @return the value type.
   */
  public FieldType<T> type() {
    return state.type();
  }

  /**
   * Whether a {@code null} input is accepted rather than reported as a missing required value. Set by {@link #optional()} and implied by {@link #defaultValue(Object)}.
   *
   * @return {@code true} if {@code null} is allowed.
   */
  public boolean isOptional() {
    return state.optional();
  }

  /**
   * Whether a {@code null} value is accepted, as set by {@link #nullable()}. Distinct from {@link #isOptional()}, which is about absence.
   *
   * @return {@code true} if {@code null} is an accepted value.
   */
  public boolean isNullable() {
    return state.nullable();
  }

  /**
   * A read-only view of the metadata attached to this schema. The map is keyed by the string form of each {@link MetaKey} and preserves insertion order.
   *
   * @return an unmodifiable view of the metadata channel.
   */
  public Map<String, Object> meta() {
    return Collections.unmodifiableMap(state.meta());
  }

  /**
   * Marks the value as optional: it may be absent. In an object an optional field drops out of the {@code required} set, and a {@code null} input validates to {@code null} rather
   * than failing as missing. For accepting {@code null} as a meaningful value (and rendering it in JSON Schema), see {@link #nullable()}.
   *
   * @return a new schema that accepts an absent value.
   */
  public SELF optional() {
    return withState(state.asOptional());
  }

  /**
   * Marks the value as required again, the inverse of {@link #optional()}: a {@code null} input is reported as a missing required value. Also clears any configured default.
   *
   * @return a new schema that rejects {@code null}.
   */
  public SELF required() {
    return withState(state.asRequired());
  }

  /**
   * Marks the value as nullable: a {@code null} input validates to {@code null} instead of failing, and JSON Schema conversion renders the type as a union with {@code "null"}.
   * This is distinct from {@link #optional()}, which governs whether the value may be <em>absent</em> (an object's required set).
   *
   * @return a new schema that accepts {@code null} as a value.
   */
  public SELF nullable() {
    return withState(state.asNullable());
  }

  /**
   * Supplies a value to use when the input is {@code null}. The default takes the place of the {@code null} before any refinements run, and setting one also makes the schema
   * {@link #optional()}.
   *
   * @param value the value substituted for a {@code null} input.
   * @return a new schema carrying the default.
   */
  public SELF defaultValue(T value) {
    return withState(state.withDefault(value));
  }

  /**
   * Adds a caller-supplied validation rule for cases the built-in refinements do not cover. The predicate receives the already-typed value and returns {@code true} when it is
   * acceptable, otherwise an issue with code {@code "custom"} and {@code message} is reported.
   *
   * @param predicate returns {@code true} for valid values.
   * @param message the failure message reported when the predicate returns {@code false}.
   * @return a new schema carrying the rule.
   */
  public SELF refine(Predicate<? super T> predicate, String message) {
    return check("custom", predicate, message);
  }

  /**
   * Produces a schema that validates with this schema and then maps the validated value through {@code mapper}, so its output type may differ from its input type. The result is a
   * terminal {@link TransformSchema}: it conforms to {@link StandardSchemaV1} and offers {@link TransformSchema#parse}/{@link TransformSchema#safeParse}, but is not itself a
   * {@link Schema} (it cannot be further refined or nested as an object field).
   *
   * @param mapper maps the validated value to the output.
   * @param <R> the output type.
   * @return a transform over this schema.
   */
  public <R> TransformSchema<T, R> transform(Function<? super T, ? extends R> mapper) {
    return new TransformSchema<>(this, mapper);
  }

  /**
   * Adds a built-in refinement carrying a stable failure {@code code}. Concrete schemas use this to implement their own verbs (for example a numeric {@code min} or a string
   * {@code regex}) so the resulting issues report a meaningful code rather than {@code "custom"}.
   *
   * @param code the failure code to report when the predicate fails.
   * @param predicate returns {@code true} for valid values.
   * @param message the failure message.
   * @return a new schema carrying the rule.
   */
  protected SELF check(String code, Predicate<? super T> predicate, String message) {
    return withState(state.plusCheck(Check.of(code, predicate, message)));
  }

  /**
   * Adds a built-in refinement that also contributes {@code jsonSchema} keywords describing the same constraint, so JSON Schema conversion stays in sync with validation (for
   * example a string {@code min} reports {@code too_small} and contributes {@code {"minLength": n}}).
   *
   * @param code the failure code to report when the predicate fails.
   * @param predicate returns {@code true} for valid values.
   * @param message the failure message.
   * @param jsonSchema the JSON Schema keywords this constraint contributes.
   * @return a new schema carrying the rule.
   */
  protected SELF check(String code, Predicate<? super T> predicate, String message, Map<String, Object> jsonSchema) {
    return withState(state.plusCheck(Check.of(code, predicate, message, jsonSchema)));
  }

  /**
   * Attaches a piece of metadata. The core ignores it, and targets read it back to drive their output. Writing an existing key replaces its value.
   *
   * @param key the metadata key, conventionally namespaced by target (for example {@code "db.pk"}).
   * @param value the value to store.
   * @return a new schema carrying the entry.
   */
  public SELF meta(String key, Object value) {
    return withState(state.plusMeta(key, value));
  }

  /**
   * Attaches a piece of metadata under a typed key, so the value type is checked at compile time.
   *
   * @param key the typed metadata key.
   * @param value the value to store.
   * @param <V> the value type the key carries.
   * @return a new schema carrying the entry.
   */
  public <V> SELF meta(MetaKey<V> key, V value) {
    return meta(key.name(), value);
  }

  /**
   * Reads a metadata value by string key.
   *
   * @param key the metadata key to read.
   * @param fallback the value to return when the key is absent.
   * @param <V> the expected value type.
   * @return the stored value, or {@code fallback} if the key is not present.
   */
  @SuppressWarnings("unchecked")
  public <V> V metaOr(String key, V fallback) {
    Map<String, Object> m = state.meta();
    return m.containsKey(key) ? (V) m.get(key) : fallback;
  }

  /**
   * Reads a metadata value by typed key.
   *
   * @param key the typed metadata key to read.
   * @param fallback the value to return when the key is absent.
   * @param <V> the value type the key carries.
   * @return the stored value, or {@code fallback} if the key is not present.
   */
  public <V> V metaOr(MetaKey<V> key, V fallback) {
    return metaOr(key.name(), fallback);
  }

  /**
   * Validates {@code input} and returns the narrowed value, throwing if anything is wrong.
   *
   * @param input the value to validate.
   * @return the validated value, which may be {@code null} when the schema is optional.
   * @throws SchemaError if the input is missing, of the wrong type, or fails any refinement. The exception carries every {@link SchemaIssue} found.
   */
  public final T parse(Object input) {
    return safeParse(input).orElseThrow();
  }

  /**
   * Validates {@code input} without throwing.
   *
   * @param input the value to validate.
   * @return a {@link ParseResult} that is either a success holding the narrowed value or a failure holding the issues.
   */
  public final ParseResult<T> safeParse(Object input) {
    return safeParse(input, SchemaPath.ROOT);
  }

  /**
   * Validates {@code input}, prefixing any reported issue paths with {@code path}. Composite schemas call this on their child schemas so a nested problem is reported at, for
   * example, {@code ["address", "zip"]}. Most callers use {@link #safeParse(Object)} instead. It is {@code public} so a composite living in another module can recurse into child
   * schemas.
   *
   * @param input the value to validate.
   * @param path the location prefix applied to any issue this validation reports.
   * @return a success holding the narrowed value, or a failure holding the issues.
   */
  public ParseResult<T> safeParse(Object input, SchemaPath path) {
    if (input == null) {
      return onNull(path);
    }
    T value;
    try {
      value = coerce(input);
    } catch (ClassCastException e) {
      return ParseResult.failure(new SchemaIssue(path, "invalid_type", "expected " + state.type().typeName()));
    }
    List<SchemaIssue> issues = new ArrayList<>();
    T processed = process(value, path, issues);
    runChecks(processed, path, issues);
    return issues.isEmpty() ? ParseResult.success(processed) : ParseResult.failure(issues);
  }

  /**
   * Exposes this schema through the Standard Schema family contracts. This is the Java stand-in for the spec's {@code "~standard"} property. The returned object conforms to both
   * {@link StandardSchemaV1.Props validation} and {@link StandardJSONSchemaV1.Props JSON Schema conversion}.
   *
   * @return the Standard Schema properties for this schema.
   */
  @Override
  public StandardProps<T, T> standard() {
    return new SchemaStandardProps();
  }

  private Map<String, Object> jsonSchemaNode(StandardJSONSchemaV1.Options options, boolean input) {
    Map<String, Object> node = jsonSchemaBody(options, input);
    if (state.nullable()) {
      applyNullable(node, options);
    }
    return node;
  }

  /**
   * Builds the JSON Schema body for this schema, its {@code type}, {@code format}, constraint keywords (contributed by its {@link Check checks}), and {@code default}, before
   * nullability is applied. Composite schemas override this to add their nested structure (an object's {@code properties}, an array's {@code items}).
   *
   * @param options the conversion options.
   * @param input whether the input ({@code true}) or output ({@code false}) shape is being rendered.
   * @return a mutable JSON Schema body.
   */
  protected Map<String, Object> jsonSchemaBody(StandardJSONSchemaV1.Options options, boolean input) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", state.type().jsonSchemaType());
    if (state.type().jsonSchemaFormat() != null) {
      node.put("format", state.type().jsonSchemaFormat());
    }
    for (Check<T> check : state.checks()) {
      node.putAll(check.jsonSchema());
    }
    if (state.hasDefault()) {
      node.put("default", state.defaultValue());
    }
    return node;
  }

  /**
   * Decides the outcome for a {@code null} input: a configured default wins, otherwise an optional schema yields {@code null} and a required one reports a {@code "required"}
   * issue.
   *
   * @param path the location prefix for a reported issue.
   * @return the result for the {@code null} case.
   */
  protected ParseResult<T> onNull(SchemaPath path) {
    if (state.hasDefault()) {
      return ParseResult.success(state.defaultValue());
    }
    if (state.optional() || state.nullable()) {
      return ParseResult.success(null);
    }
    return ParseResult.failure(new SchemaIssue(path, "required", "required value was null"));
  }

  /**
   * Narrows a non-null input to {@code T} before refinements run, throwing {@link ClassCastException} if the input is the wrong type (which {@link #safeParse(Object, SchemaPath)}
   * turns into an {@code "invalid_type"} issue). The default delegates to {@link FieldType#cast}, and composites override it to accept their container type. (See
   * {@link #safeParse(Object, SchemaPath)}.)
   *
   * @param input the non-null input.
   * @return the input narrowed to {@code T}.
   */
  protected T coerce(Object input) {
    return state.type().cast(input);
  }

  /**
   * Validates and, for composites, rebuilds the structure of a value after it has been narrowed but before refinements run. A scalar schema returns the value unchanged. An
   * {@code object} or {@code array} recurses into its children here, appending their problems to {@code issues} and returning a validated copy.
   *
   * @param value the narrowed value.
   * @param path the location prefix for any nested issue.
   * @param issues the collector to append problems to.
   * @return the value to validate further and return (a rebuilt copy for composites).
   */
  protected T process(T value, SchemaPath path, List<SchemaIssue> issues) {
    return value;
  }

  /**
   * Runs every configured refinement against {@code value} and records the message of each that fails.
   *
   * @param value the value to check.
   * @param path the location prefix for any reported issue.
   * @param issues the collector to append failures to.
   */
  protected void runChecks(T value, SchemaPath path, List<SchemaIssue> issues) {
    for (Check<T> check : state.checks()) {
      String message = check.validate(value);
      if (message != null) {
        issues.add(new SchemaIssue(path, check.code(), message));
      }
    }
  }

  /**
   * The combined Standard Schema properties a {@link Schema} exposes: every family contract it implements at once. This single type lets one {@link #standard()} accessor satisfy
   * both {@link StandardSchemaV1} and {@link StandardJSONSchemaV1}.
   *
   * @param <I> the accepted input type.
   * @param <O> the produced output type.
   */
  public interface StandardProps<I, O> extends StandardSchemaV1.Props<I, O>, StandardJSONSchemaV1.Props<I, O> {
  }

  /**
   * The universal state shared by every schema, bundled into one record so a subtype only carries one field and implements a single {@link #withState} hook. The instance methods
   * return copies with one aspect changed, which is how the schema stays immutable.
   *
   * @param type the value type.
   * @param optional whether the value may be absent (drives an object's {@code required} set).
   * @param nullable whether the value may be {@code null} (drives JSON Schema's null union).
   * @param hasDefault whether a default value has been set.
   * @param defaultValue the value substituted for {@code null} input when {@code hasDefault} is true.
   * @param checks the refinements to run, in declaration order.
   * @param meta the metadata channel.
   * @param <T> the value type.
   */
  protected static final class State<T> {

    private final FieldType<T> type;
    private final boolean optional;
    private final boolean nullable;
    private final boolean hasDefault;
    private final T defaultValue;
    private final List<Check<T>> checks;
    private final Map<String, Object> meta;

    State(FieldType<T> type, boolean optional, boolean nullable, boolean hasDefault, T defaultValue, List<Check<T>> checks, Map<String, Object> meta) {
      this.type = type;
      this.optional = optional;
      this.nullable = nullable;
      this.hasDefault = hasDefault;
      this.defaultValue = defaultValue;
      this.checks = checks;
      this.meta = meta;
    }

    public FieldType<T> type() {
      return type;
    }

    public boolean optional() {
      return optional;
    }

    public boolean nullable() {
      return nullable;
    }

    public boolean hasDefault() {
      return hasDefault;
    }

    public T defaultValue() {
      return defaultValue;
    }

    public List<Check<T>> checks() {
      return checks;
    }

    public Map<String, Object> meta() {
      return meta;
    }

    /**
     * Starts the state for a required schema with no default, no checks, and an empty metadata channel.
     *
     * @param type the value type.
     * @param <T> the value type.
     * @return fresh state.
     */
    public static <T> State<T> of(FieldType<T> type) {
      return new State<>(type, false, false, false, null, List.of(), new LinkedHashMap<>());
    }

    State<T> asOptional() {
      return new State<>(type, true, nullable, hasDefault, defaultValue, checks, meta);
    }

    State<T> asNullable() {
      return new State<>(type, optional, true, hasDefault, defaultValue, checks, meta);
    }

    State<T> asRequired() {
      return new State<>(type, false, nullable, false, null, checks, meta);
    }

    State<T> withDefault(T value) {
      return new State<>(type, true, nullable, true, value, checks, meta);
    }

    State<T> plusCheck(Check<T> check) {
      List<Check<T>> next = new ArrayList<>(checks);
      next.add(check);
      return new State<>(type, optional, nullable, hasDefault, defaultValue, List.copyOf(next), meta);
    }

    State<T> plusMeta(String key, Object value) {
      Map<String, Object> next = new LinkedHashMap<>(meta);
      next.put(key, value);
      return new State<>(type, optional, nullable, hasDefault, defaultValue, checks, next);
    }
  }

  private final class SchemaStandardProps implements StandardProps<T, T> {

    @Override
    public int version() {
      return 1;
    }

    @Override
    public String vendor() {
      return VENDOR;
    }

    @Override
    public StandardTypedV1.Types<T, T> types() {
      Class<T> carrier = state.type().javaType();
      return new StandardTypedV1.Types<>(carrier, carrier);
    }

    @Override
    public CompletableFuture<StandardSchemaV1.Result<T>> validate(Object value, StandardSchemaV1.Options options) {
      return CompletableFuture.completedFuture(toStandardResult(safeParse(value)));
    }

    @Override
    public StandardJSONSchemaV1.Converter jsonSchema() {
      return new StandardJSONSchemaV1.Converter() {
        @Override
        public Map<String, Object> input(StandardJSONSchemaV1.Options options) {
          return jsonSchemaNode(options, true);
        }

        @Override
        public Map<String, Object> output(StandardJSONSchemaV1.Options options) {
          return jsonSchemaNode(options, false);
        }
      };
    }
  }
}
