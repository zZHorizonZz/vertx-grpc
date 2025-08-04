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
package io.vertx.jrpc.transcoding.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.impl.GrpcHttpInvoker;
import io.vertx.grpc.server.impl.GrpcInvocation;
import io.vertx.grpc.server.impl.GrpcServerRequestImpl;
import io.vertx.grpc.server.impl.GrpcServerResponseImpl;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;

/**
 * Implementation of GrpcHttpInvoker for JSON-RPC transcoding.
 * <p>
 * This class handles the conversion of HTTP requests to JSON-RPC requests and then to gRPC invocations. It is registered with the Vert.x gRPC server through the Java Service
 * Provider Interface (SPI) and is automatically detected and used when a JSON-RPC request is received.
 * <p>
 * The invoker accepts HTTP POST requests with a Content-Type of application/json and creates a GrpcInvocation that will handle the JSON-RPC request. The actual parsing of the
 * JSON-RPC request is deferred until the request body is available.
 * <p>
 * This implementation supports:
 * <ul>
 *   <li>JSON-RPC 2.0 requests with positional or named parameters</li>
 *   <li>JSON-RPC 2.0 notifications (requests without an id)</li>
 *   <li>JSON-RPC 2.0 batch requests</li>
 *   <li>Error handling according to the JSON-RPC 2.0 specification</li>
 * </ul>
 */
public class JrpcHttpInvoker implements GrpcHttpInvoker {

  /**
   * Accepts an HTTP request and creates a GrpcInvocation if the request is a valid JSON-RPC request.
   * <p>
   * This method checks if the request is a POST request with a Content-Type of application/json. If it is, it creates a JrpcTranscodingServerRequest and
   * JrpcTranscodingServerResponse to handle the request and returns a GrpcInvocation that will invoke the service method.
   * <p>
   * If the request is not a valid JSON-RPC request, this method returns null, allowing other invokers to handle the request.
   *
   * @param request the HTTP server request
   * @param serviceMethod the service method to invoke
   * @return a GrpcInvocation if the request is a valid JSON-RPC request, null otherwise
   */
  @Override
  public <Req, Resp> GrpcInvocation<Req, Resp> accept(HttpServerRequest request, ServiceMethod<Req, Resp> serviceMethod) {
    // Only accept POST requests with application/json content type
    if (!request.method().name().equals("POST")) {
      return null;
    }

    String contentType = request.getHeader("Content-Type");
    if (contentType == null || !contentType.startsWith("application/json")) {
      return null;
    }

    // Read the request body and create a JSON-RPC request
    try {
      // Get the context
      ContextInternal context = ((HttpServerRequestInternal) request).context();

      // Create a method call
      GrpcMethodCall methodCall = new GrpcMethodCall(serviceMethod.methodName());

      // Create the request and response objects
      // We'll parse the JSON-RPC request in the JrpcTranscodingServerRequest
      GrpcServerRequestImpl<Req, Resp> grpcRequest = new JrpcTranscodingServerRequest<>(
        context,
        request,
        null,
        serviceMethod.decoder(),
        methodCall
      );

      GrpcServerResponseImpl<Req, Resp> grpcResponse = new JrpcTranscodingServerResponse<>(
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

  private String extractMethodName(JsonRpcRequest request, HttpServerRequest httpServerRequest) {
    if (httpServerRequest.uri().equals("/") || httpServerRequest.uri().equals("/*")) {
      if (request.getMethod().contains("/")) {
        return request.getMethod();
      }
    }

    if (request.getMethod() == null) {
      return null;
    }

    if (httpServerRequest.uri().endsWith("/")) {
      return httpServerRequest.uri() + request.getMethod();
    }

    return httpServerRequest.uri() + "/" + request.getMethod();
  }
}
