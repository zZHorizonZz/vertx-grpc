/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.server.impl;

import io.vertx.grpc.server.GrpcCorsOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Matches a request {@code Origin} against the {@link GrpcCorsOptions} allow-list. Self-contained so that
 * vertx-grpc-server does not depend on vertx-web for CORS handling.
 */
final class CorsOriginMatcher {

  private final boolean allowAll;
  private final Set<String> exactOrigins;
  private final List<Pattern> originPatterns;

  CorsOriginMatcher(GrpcCorsOptions options) {
    boolean all = false;
    Set<String> exact = new HashSet<>();
    for (String origin : options.getAllowedOrigins()) {
      if ("*".equals(origin)) {
        all = true;
      } else {
        exact.add(origin);
      }
    }
    List<Pattern> patterns = new ArrayList<>();
    for (String pattern : options.getAllowedOriginPatterns()) {
      patterns.add(Pattern.compile(pattern));
    }
    this.allowAll = all;
    this.exactOrigins = exact;
    this.originPatterns = patterns;
  }

  /**
   * @return whether any origin is allowed ({@code *} was configured)
   */
  boolean allowAll() {
    return allowAll;
  }

  /**
   * @param origin the request origin, may be {@code null}
   * @return whether the origin is allowed to perform a cross-origin request
   */
  boolean isAllowed(String origin) {
    if (origin == null) {
      return false;
    }
    if (allowAll || exactOrigins.contains(origin)) {
      return true;
    }
    for (Pattern pattern : originPatterns) {
      if (pattern.matcher(origin).matches()) {
        return true;
      }
    }
    return false;
  }
}
