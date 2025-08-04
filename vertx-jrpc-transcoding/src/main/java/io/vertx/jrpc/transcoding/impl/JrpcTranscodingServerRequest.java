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

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.impl.GrpcServerRequestImpl;
import io.vertx.grpc.transcoding.impl.TranscodingGrpcServerRequest;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;

import java.util.List;

/**
 * Implementation of GrpcServerRequestImpl for JSON-RPC transcoding.
 * <p>
 * This class handles the conversion of JSON-RPC requests to gRPC messages.
 *
 * @param <Req> the request type
 * @param <Resp> the response type
 */
public class JrpcTranscodingServerRequest<Req, Resp> extends TranscodingGrpcServerRequest<Req, Resp> {

  private final String methodName;
  private final HttpServerRequest httpRequest;
  private final GrpcMessageDecoder<Req> messageDecoder;

  private JsonRpcRequest jsonRpcRequest;

  /**
   * Creates a new JrpcTranscodingServerRequest.
   *
   * @param context the Vert.x context
   * @param httpRequest the HTTP server request
   * @param jsonRpcRequest the JSON-RPC request
   * @param messageDecoder the message decoder
   * @param methodCall the gRPC method call
   */
  public JrpcTranscodingServerRequest(ContextInternal context, HttpServerRequest httpRequest, JsonRpcRequest jsonRpcRequest, GrpcMessageDecoder<Req> messageDecoder,
    GrpcMethodCall methodCall) {
    super(context, httpRequest, "", List.of(), messageDecoder, methodCall);

    this.httpRequest = httpRequest;
    this.jsonRpcRequest = jsonRpcRequest;
    this.methodName = methodCall.methodName();
    this.messageDecoder = createMessageDecoder(httpRequest, messageDecoder, jsonRpcRequest, methodCall);
  }

  @Override
  public GrpcServerRequestImpl<Req, Resp> handler(Handler<Req> handler) {
    if (httpRequest instanceof HttpProxyServerRequest) {
      try {
        Req req = messageDecoder.decode(GrpcMessage.message("identity", WireFormat.JSON, ((HttpProxyServerRequest) httpRequest).getTransformedBody()));
        handler.handle(req);
      } catch (Exception e) {
        super.tryFail(e);
      }
    } else {
      super.handler(handler);
    }
    return this;
  }

  private static <Req> GrpcMessageDecoder<Req> createMessageDecoder(HttpServerRequest request, GrpcMessageDecoder<Req> originalDecoder, JsonRpcRequest jsonRpcRequest,
    GrpcMethodCall methodCall) {
    return new GrpcMessageDecoder<>() {
      private JsonRpcRequest parsedRequest = jsonRpcRequest;

      @Override
      public Req decode(GrpcMessage msg) throws CodecException {
        if (request instanceof HttpProxyServerRequest) {
          parsedRequest = ((HttpProxyServerRequest) request).getJsonRpcRequest();
          if (parsedRequest != null && !parsedRequest.getMethod().equalsIgnoreCase(methodCall.methodName())) {
            throw new CodecException("Method not found: " + parsedRequest.getMethod());
          }
        }
        // If we don't have a JSON-RPC request yet, parse it from the message
        if (parsedRequest == null) {
          try {
            String jsonStr = msg.payload().toString();
            JsonObject jsonObject = new JsonObject(jsonStr);
            parsedRequest = JsonRpcRequest.fromJson(jsonObject);

            // Check if the method matches
            if (!parsedRequest.getMethod().equalsIgnoreCase(methodCall.methodName())) {
              throw new CodecException("Method not found: " + parsedRequest.getMethod());
            }
          } catch (DecodeException | IllegalArgumentException e) {
            throw new CodecException(e);
          }
        }

        Buffer jsonBuffer;
        try {
          // Convert the JSON-RPC params to a format the service method can understand
          Object params = parsedRequest.getParams();
          if (params == null) {
            // If no params provided, use an empty object
            jsonBuffer = Buffer.buffer("{}");
          } else {
            // Use the params as-is (could be JsonObject or JsonArray)
            jsonBuffer = Buffer.buffer(params.toString());
          }
        } catch (DecodeException e) {
          throw new CodecException(e);
        }

        return originalDecoder.decode(GrpcMessage.message("identity", parsedRequest.getNamedParams() != null ? WireFormat.JSON : WireFormat.JSON_ARRAY, jsonBuffer));
      }

      @Override
      public boolean accepts(WireFormat format) {
        return originalDecoder.accepts(format);
      }
    };
  }

  /**
   * @return the JSON-RPC request
   */
  public JsonRpcRequest getJsonRpcRequest() {
    return httpRequest instanceof HttpProxyServerRequest ? ((HttpProxyServerRequest) httpRequest).getJsonRpcRequest() : jsonRpcRequest;
  }
}
