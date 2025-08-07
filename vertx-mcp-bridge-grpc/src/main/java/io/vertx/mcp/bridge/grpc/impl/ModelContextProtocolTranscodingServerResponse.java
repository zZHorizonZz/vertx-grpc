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

import io.vertx.core.Future;
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
import io.vertx.grpc.transcoding.impl.GrpcTranscodingError;
import io.vertx.mcp.jrpc.model.JsonRpcError;
import io.vertx.mcp.jrpc.model.JsonRpcResponse;

import java.util.Date;

/**
 * Implementation of GrpcServerResponseImpl for JSON-RPC transcoding.
 * <p>
 * This class handles the conversion of gRPC responses to JSON-RPC responses.
 *
 * @param <Req> the request type
 * @param <Resp> the response type
 */
public class ModelContextProtocolTranscodingServerResponse<Req, Resp> extends GrpcServerResponseImpl<Req, Resp> {

  private final ModelContextProtocolTranscodingServerRequest<Req, Resp> request;
  private final HttpServerResponse httpResponse;

  /**
   * Creates a new ModelContextProtocolTranscodingServerResponse.
   *
   * @param context the Vert.x context
   * @param request the gRPC server request
   * @param httpResponse the HTTP server response
   * @param encoder the message encoder
   */
  public ModelContextProtocolTranscodingServerResponse(ContextInternal context,
    GrpcServerRequestImpl<Req, Resp> request,
    HttpServerResponse httpResponse,
    GrpcMessageEncoder<Resp> encoder) {
    super(context, request, GrpcProtocol.TRANSCODING, httpResponse, encoder);

    this.request = (ModelContextProtocolTranscodingServerRequest<Req, Resp>) request;
    this.httpResponse = httpResponse;
  }

  @Override
  protected Future<Void> sendMessage(Buffer message, boolean compressed) {
    try {
      // Convert the gRPC response to a JSON-RPC response
      JsonObject responseJson = new JsonObject(message.toString());

      // Create a JSON-RPC response with the result and the original request ID
      JsonRpcResponse jsonRpcResponse = new JsonRpcResponse(
        responseJson,
        request.getJsonRpcRequest().getId()
      );

      // Convert to JSON and send
      Buffer responseBuffer = Buffer.buffer("event: message\n" +
        "data: " + jsonRpcResponse.toJson().encode() + "\n\n");
      //httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(responseBuffer.length()));
      httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream");
      httpResponse.putHeader(HttpHeaders.DATE, Date.from(new Date().toInstant()).toString());
      httpResponse.putHeader(HttpHeaders.CONNECTION, "keep-alive");
      httpResponse.putHeader(HttpHeaders.KEEP_ALIVE, "timeout=5");
      httpResponse.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      httpResponse.putHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Mcp-Session-Id");
      httpResponse.setChunked(true);

      return httpResponse.write(responseBuffer);
    } catch (Exception e) {
      JsonRpcResponse errorResponse = new JsonRpcResponse(
        JsonRpcError.internalError(e.getMessage()),
        request.getJsonRpcRequest().getId()
      );

      Buffer errorBuffer = Buffer.buffer(errorResponse.toJson().encode());
      httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(errorBuffer.length()));
      httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      httpResponse.setStatusCode(200); // JSON-RPC always uses 200 OK, errors are in the response body
      return httpResponse.end(errorBuffer);
    }
  }

  @Override
  protected Future<Void> sendEnd() {
    GrpcStatus status = status();
    if (status != GrpcStatus.OK) {
      httpResponse.setStatusCode(GrpcTranscodingError.fromHttp2Code(status.code).getHttpStatusCode());
    }
    return super.sendEnd();
  }

  @Override
  public void cancel() {
    sendResponse(new JsonRpcResponse(
      JsonRpcError.internalError("Request cancelled"),
      request.getJsonRpcRequest().getId()
    ));

    // Call the parent cancel method to handle internal state
    super.cancel();
  }

  public ModelContextProtocolTranscodingServerResponse<Req, Resp> sendResponse(JsonRpcResponse response) {
    Buffer errorBuffer = Buffer.buffer(response.toJson().encode());
    httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(errorBuffer.length()));
    httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpResponse.setStatusCode(200); // JSON-RPC always uses 200 OK, errors are in the response body
    httpResponse.end(errorBuffer);
    return this;
  }
}
