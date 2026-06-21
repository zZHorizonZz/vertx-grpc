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

import java.util.Objects;

/**
 * A typed key into a {@link Schema}'s metadata channel. Targets declare their own keys (for example {@code db.pk}, {@code pb.number}, {@code gql.deprecated}) so reads and writes
 * are type-checked instead of stringly-typed.
 *
 * <pre>{@code
 * static final MetaKey<Boolean> PK = MetaKey.of("db.pk");
 * Schema<?, ?> writable = field.meta(PK, true);
 * boolean primaryKey = writable.metaOr(PK, false);
 * }</pre>
 *
 * @param name the underlying string key, conventionally namespaced ({@code "<target>.<name>"}).
 * @param <V> the value type stored under this key.
 */
public final class MetaKey<V> {

  private final String name;

  public MetaKey(String name) {
    this.name = name;
  }

  /**
   * Creates a key.
   *
   * @param name the namespaced string key.
   * @param <V> the value type.
   * @return a new key.
   */
  public static <V> MetaKey<V> of(String name) {
    return new MetaKey<>(name);
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetaKey)) {
      return false;
    }
    MetaKey<?> metaKey = (MetaKey<?>) o;
    return Objects.equals(name, metaKey.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "MetaKey[name=" + name + "]";
  }
}
