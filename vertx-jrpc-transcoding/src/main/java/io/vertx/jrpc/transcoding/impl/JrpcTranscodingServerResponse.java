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

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcProtocol;
import io.vertx.grpc.server.impl.GrpcServerRequestImpl;
import io.vertx.grpc.server.impl.GrpcServerResponseImpl;
import io.vertx.jrpc.transcoding.model.JsonRpcError;
import io.vertx.jrpc.transcoding.model.JsonRpcResponse;

/**
 * Implementation of GrpcServerResponseImpl for JSON-RPC transcoding.
 * <p>
 * This class handles the conversion of gRPC responses to JSON-RPC responses.
 *
 * @param <Req> the request type
 * @param <Resp> the response type
 */
public class JrpcTranscodingServerResponse<Req, Resp> extends GrpcServerResponseImpl<Req, Resp> {

  private final JrpcTranscodingServerRequest<Req, Resp> request;
  private final HttpServerResponse httpResponse;
  private Promise<Void> head;

  /**
   * Creates a new JrpcTranscodingServerResponse.
   *
   * @param context the Vert.x context
   * @param request the gRPC server request
   * @param httpResponse the HTTP server response
   * @param encoder the message encoder
   */
  public JrpcTranscodingServerResponse(ContextInternal context,
                                      GrpcServerRequestImpl<Req, Resp> request,
                                      HttpServerResponse httpResponse,
                                      GrpcMessageEncoder<Resp> encoder) {
    super(context, request, GrpcProtocol.TRANSCODING, httpResponse, encoder);

    this.request = (JrpcTranscodingServerRequest<Req, Resp>) request;
    this.httpResponse = httpResponse;
  }

  @Override
  protected Future<Void> sendHead() {
    head = context.promise();
    return head.future();
  }

  @Override
  protected Future<Void> sendMessage(Buffer message, boolean compressed) {
    Future<Void> res;
    try {
      // Convert the gRPC response to a JSON-RPC response
      JsonObject responseJson = new JsonObject(message.toString());

      // Create a JSON-RPC response with the result and the original request ID
      JsonRpcResponse jsonRpcResponse = new JsonRpcResponse(
        responseJson,
        request.getJsonRpcRequest().getId()
      );

      // Convert to JSON and send
      Buffer responseBuffer = Buffer.buffer(jsonRpcResponse.toJson().encode());
      httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(responseBuffer.length()));
      httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      res = httpResponse.write(responseBuffer);
    } catch (Exception e) {
      // Create an error response
      JsonRpcResponse errorResponse = new JsonRpcResponse(
        JsonRpcError.internalError(e.getMessage()),
        request.getJsonRpcRequest().getId()
      );

      Buffer errorBuffer = Buffer.buffer(errorResponse.toJson().encode());
      httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(errorBuffer.length()));
      httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      httpResponse.setStatusCode(200); // JSON-RPC always uses 200 OK, errors are in the response body
      res = httpResponse.write(errorBuffer);
    }

    if (head != null) {
      res.onComplete(head);
    }
    return res;
  }

  @Override
  protected void encodeGrpcHeaders(MultiMap grpcHeaders, MultiMap httpHeaders) {
    // No gRPC headers needed for JSON-RPC
  }

  @Override
  protected Future<Void> sendEnd() {
    return super.sendEnd();
  }

  @Override
  public void cancel() {
    // Create a JSON-RPC error response for cancellation
    JsonRpcResponse errorResponse = new JsonRpcResponse(
      JsonRpcError.internalError("Request cancelled"),
      request.getJsonRpcRequest().getId()
    );

    Buffer errorBuffer = Buffer.buffer(errorResponse.toJson().encode());
    httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(errorBuffer.length()));
    httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpResponse.setStatusCode(200); // JSON-RPC always uses 200 OK, errors are in the response body
    httpResponse.end(errorBuffer);

    // Call the parent cancel method to handle internal state
    super.cancel();
  }
}
