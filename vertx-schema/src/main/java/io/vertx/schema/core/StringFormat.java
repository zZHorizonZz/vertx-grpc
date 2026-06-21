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

import java.util.Map;
import java.util.regex.Pattern;

/**
 * A reusable string format fed to {@link StringSchema#matches(StringFormat)}, holding a compiled {@link Pattern}, a label for the failure message, and the JSON Schema keywords
 * that describe it. It is a descriptor, not a schema, so it does not validate on its own. Build one from a custom expression with {@code format("[a-z]+")}, or from one of the
 * named presets such as {@code format().email()}, {@code format().uuid()}, or {@code format().datetime()}:
 *
 * <pre>{@code
 * import static io.vertx.schema.core.StringFormat.format;
 *
 * Schemas.string().matches(format().email());
 * Schemas.string().matches(format("[a-z]+"));
 * }</pre>
 *
 * <p>Presets that correspond to a registered JSON Schema {@code format} keyword (email, uri, uuid, hostname, the ISO
 * date/time family, ipv4/ipv6) contribute that {@code format}. The rest contribute their {@code pattern}. The patterns are pragmatic rather than RFC-exhaustive, so they screen out
 * obvious mistakes.
 */
public final class StringFormat {

  private final Pattern pattern;
  private final String description;
  private final Map<String, Object> jsonSchema;

  private StringFormat(Pattern pattern, String description, Map<String, Object> jsonSchema) {
    this.pattern = pattern;
    this.description = description;
    this.jsonSchema = jsonSchema;
  }

  /**
   * A custom pattern. The value must match it in full (anchored at both ends).
   *
   * @param pattern the regular expression.
   * @return a format carrying the compiled pattern and a {@code "pattern"} JSON Schema fragment.
   */
  public static StringFormat format(String pattern) {
    return new StringFormat(Pattern.compile(pattern), pattern, Map.of("pattern", pattern));
  }

  /**
   * The named presets, reached as {@code format().email()}, {@code format().uuid()}, and so on.
   *
   * @return the preset builder.
   */
  public static Presets format() {
    return Presets.INSTANCE;
  }

  public Pattern pattern() {
    return pattern;
  }

  public String description() {
    return description;
  }

  public Map<String, Object> jsonSchema() {
    return jsonSchema;
  }

  public static final class Presets {

    private static final Presets INSTANCE = new Presets();

    private Presets() {
    }

    private static StringFormat formatted(String regex, String name, String jsonFormat) {
      return new StringFormat(Pattern.compile(regex), name, Map.of("format", jsonFormat));
    }

    private static StringFormat patterned(String regex, String name) {
      return new StringFormat(Pattern.compile(regex), name, Map.of("pattern", regex));
    }

    public StringFormat email() {
      return formatted("[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "email", "email");
    }

    public StringFormat url() {
      return formatted("https?://[^\\s]+", "url", "uri");
    }

    public StringFormat uuid() {
      return formatted("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "uuid", "uuid");
    }

    public StringFormat hostname() {
      return formatted("[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*", "hostname", "hostname");
    }

    public StringFormat ipv4() {
      return formatted("(\\d{1,3}\\.){3}\\d{1,3}", "ipv4", "ipv4");
    }

    public StringFormat ipv6() {
      return formatted("([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}", "ipv6", "ipv6");
    }

    public StringFormat date() {
      return formatted("\\d{4}-\\d{2}-\\d{2}", "date", "date");
    }

    public StringFormat time() {
      return formatted("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?", "time", "time");
    }

    public StringFormat datetime() {
      return formatted("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?", "datetime", "date-time");
    }

    public StringFormat duration() {
      return formatted("P(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+(\\.\\d+)?S)?)?", "duration", "duration");
    }

    public StringFormat cuid() {
      return patterned("c[a-z0-9]{8,}", "cuid");
    }

    public StringFormat cuid2() {
      return patterned("[a-z][a-z0-9]{7,}", "cuid2");
    }

    public StringFormat ulid() {
      return patterned("[0-9A-HJKMNP-TV-Z]{26}", "ulid");
    }

    public StringFormat nanoid() {
      return patterned("[a-zA-Z0-9_-]{21}", "nanoid");
    }

    public StringFormat jwt() {
      return patterned("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*", "jwt");
    }

    public StringFormat base64() {
      return patterned("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?", "base64");
    }

    public StringFormat base64url() {
      return patterned("[A-Za-z0-9_-]+", "base64url");
    }

    public StringFormat hex() {
      return patterned("[0-9a-fA-F]+", "hex");
    }

    public StringFormat e164() {
      return patterned("\\+[1-9]\\d{1,14}", "e164");
    }

    public StringFormat mac() {
      return patterned("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}", "mac");
    }

    public StringFormat cidrv4() {
      return patterned("(\\d{1,3}\\.){3}\\d{1,3}/\\d{1,2}", "cidrv4");
    }

    public StringFormat cidrv6() {
      return patterned("[0-9a-fA-F:]+/\\d{1,3}", "cidrv6");
    }
  }
}
