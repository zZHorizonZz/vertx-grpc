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

import io.vertx.core.http.HttpMethod;

import java.util.Collections;
import java.util.Set;

/**
 * Information contributed by a {@link GrpcHttpInvoker} when the server answers an {@code OPTIONS} request.
 * <p>
 * An invoker does not write the preflight response itself: the server aggregates the contributions of every
 * invoker, together with the built-in gRPC protocols and the CORS configuration, and writes a single response.
 * This keeps the server the only writer and lets an invoker that does not implement preflight degrade to
 * {@link #EMPTY} without producing a partial response.
 */
public final class PreflightInfo {

  /**
   * An empty contribution: no allowed methods and no accepted POST media types.
   */
  public static final PreflightInfo EMPTY = new PreflightInfo(Collections.emptySet(), Collections.emptySet());

  private final Set<HttpMethod> methods;
  private final Set<String> acceptPostMediaTypes;

  public PreflightInfo(Set<HttpMethod> methods, Set<String> acceptPostMediaTypes) {
    this.methods = methods;
    this.acceptPostMediaTypes = acceptPostMediaTypes;
  }

  /**
   * @return the HTTP methods bound at the request path, contributing to the {@code Allow} and
   *         {@code Access-Control-Allow-Methods} headers
   */
  public Set<HttpMethod> methods() {
    return methods;
  }

  /**
   * @return the media types accepted for {@code POST} at the request path, contributing to the
   *         {@code Accept-Post} header
   */
  public Set<String> acceptPostMediaTypes() {
    return acceptPostMediaTypes;
  }
}
