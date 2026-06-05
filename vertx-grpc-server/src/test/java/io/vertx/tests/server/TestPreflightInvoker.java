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
package io.vertx.tests.server;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.impl.GrpcHttpInvoker;
import io.vertx.grpc.server.impl.GrpcInvocation;
import io.vertx.grpc.server.impl.PreflightInfo;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A test {@link GrpcHttpInvoker} that does not invoke anything ({@link #accept} returns {@code null}) but contributes a
 * known preflight result, so the server preflight aggregation can be verified without depending on a real invoker such
 * as transcoding. It mimics the transcoding passthrough by accepting {@code application/json} over {@code POST}, plus a
 * {@code GET} binding so the {@code Allow} verb union is observable.
 */
public class TestPreflightInvoker implements GrpcHttpInvoker {

  @Override
  public <Req, Resp> GrpcInvocation accept(HttpServerRequest request, ServiceMethod<Req, Resp> serviceMethod) {
    return null;
  }

  @Override
  public PreflightInfo preflight(HttpServerRequest request, ServiceMethod<?, ?> serviceMethod) {
    if (!request.path().equals("/" + serviceMethod.fullMethodName())) {
      return PreflightInfo.EMPTY;
    }
    Set<HttpMethod> methods = new LinkedHashSet<>();
    methods.add(HttpMethod.GET);
    methods.add(HttpMethod.POST);
    Map<HttpMethod, Set<String>> acceptedMediaTypes = new LinkedHashMap<>();
    acceptedMediaTypes.put(HttpMethod.POST, new LinkedHashSet<>(Arrays.asList("application/json")));
    return new PreflightInfo(methods, acceptedMediaTypes);
  }
}
