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

import java.util.List;

/**
 * Thrown by {@link Schema#parse(Object)} when validation fails. Carries every {@link SchemaIssue} found, not just the first. Use {@link Schema#safeParse(Object)} to get them
 * without an exception.
 */
public final class SchemaError extends RuntimeException {

  private final transient List<SchemaIssue> issues;

  /**
   * @param issues the validation problems, which must be non-empty.
   */
  public SchemaError(List<SchemaIssue> issues) {
    super(summarize(issues));
    this.issues = List.copyOf(issues);
  }

  private static String summarize(List<SchemaIssue> issues) {
    if (issues.isEmpty()) {
      return "validation failed";
    }
    SchemaIssue first = issues.get(0);
    String head = first.toString();
    return issues.size() == 1 ? head : head + " (and " + (issues.size() - 1) + " more)";
  }

  public List<SchemaIssue> issues() {
    return issues;
  }
}
