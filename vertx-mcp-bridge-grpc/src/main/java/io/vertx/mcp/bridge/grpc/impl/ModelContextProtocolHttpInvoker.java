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
package io.vertx.mcp.bridge.grpc.impl;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.impl.GrpcHttpInvoker;
import io.vertx.grpc.server.impl.GrpcInvocation;
import io.vertx.grpc.server.impl.GrpcServerRequestImpl;
import io.vertx.grpc.server.impl.GrpcServerResponseImpl;

public class ModelContextProtocolHttpInvoker implements GrpcHttpInvoker {

  @Override
  public <Req, Resp> GrpcInvocation<Req, Resp> accept(HttpServerRequest request, ServiceMethod<Req, Resp> serviceMethod) {
    if (!(request instanceof ProxyHttpServerRequestRequest)) {
      return null;
    }

    String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
    String acceptContentType = request.getHeader(HttpHeaders.ACCEPT);

    if (contentType == null || !contentType.startsWith("application/json")) {
      return null;
    }

    if (acceptContentType == null || (!acceptContentType.contains("application/json") && !acceptContentType.contains("text/event-stream"))) {
      return null;
    }

    try {
      ContextInternal context = ((HttpServerRequestInternal) request).context();
      GrpcMethodCall methodCall = new GrpcMethodCall(serviceMethod.methodName());

      GrpcServerRequestImpl<Req, Resp> grpcRequest = new ModelContextProtocolTranscodingServerRequest<>(
        context,
        request,
        ((ProxyHttpServerRequestRequest) request).getJsonRpcRequest(),
        serviceMethod.decoder(),
        methodCall
      );

      GrpcServerResponseImpl<Req, Resp> grpcResponse = new ModelContextProtocolTranscodingServerResponse<>(
        context,
        grpcRequest,
        request.response(),
        serviceMethod.encoder()
      );

      return new GrpcInvocation<>(grpcRequest, grpcResponse);
    } catch (Exception e) {
      return null;
    }
  }
}
