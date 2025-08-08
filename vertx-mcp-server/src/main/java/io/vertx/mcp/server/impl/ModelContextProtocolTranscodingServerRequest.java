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
package io.vertx.mcp.server.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.impl.GrpcServerRequestImpl;
import io.vertx.grpc.transcoding.impl.TranscodingGrpcServerRequest;
import io.vertx.mcp.jrpc.model.JsonRpcRequest;

import java.util.List;

class ModelContextProtocolTranscodingServerRequest<Req, Resp> extends TranscodingGrpcServerRequest<Req, Resp> {

  private final String methodName;
  private final JsonRpcRequest jsonRpcRequest;
  private final HttpServerRequest httpRequest;
  private final GrpcMessageDecoder<Req> messageDecoder;

  ModelContextProtocolTranscodingServerRequest(ContextInternal context, HttpServerRequest httpRequest, JsonRpcRequest jsonRpcRequest, GrpcMessageDecoder<Req> messageDecoder,
    GrpcMethodCall methodCall) {
    super(context, httpRequest, "", List.of(), messageDecoder, methodCall);

    this.httpRequest = httpRequest;
    this.jsonRpcRequest = jsonRpcRequest;
    this.methodName = methodCall.methodName();
    this.messageDecoder = createMessageDecoder(messageDecoder, jsonRpcRequest);
  }

  @Override
  public GrpcServerRequestImpl<Req, Resp> handler(Handler<Req> handler) {
    if (httpRequest instanceof ProxyHttpServerRequestRequest) {
      try {
        Req req = messageDecoder.decode(GrpcMessage.message("identity", WireFormat.JSON, ((ProxyHttpServerRequestRequest) httpRequest).getTransformedBody()));
        handler.handle(req);
      } catch (Exception e) {
        super.tryFail(e);
      }
    } else {
      super.handler(handler);
    }
    return this;
  }

  private static <Req> GrpcMessageDecoder<Req> createMessageDecoder(GrpcMessageDecoder<Req> originalDecoder, JsonRpcRequest jsonRpcRequest) {
    return new GrpcMessageDecoder<>() {

      @Override
      public Req decode(GrpcMessage msg) throws CodecException {
        Buffer jsonBuffer;
        try {
          Object params = jsonRpcRequest.getParams();
          if (params == null) {
            jsonBuffer = Buffer.buffer("{}");
          } else {
            jsonBuffer = Buffer.buffer(params.toString());
          }
        } catch (DecodeException e) {
          throw new CodecException(e);
        }

        return originalDecoder.decode(GrpcMessage.message("identity", jsonRpcRequest.getNamedParams() != null ? WireFormat.JSON : WireFormat.JSON_ARRAY, jsonBuffer));
      }

      @Override
      public boolean accepts(WireFormat format) {
        return originalDecoder.accepts(format);
      }
    };
  }

  public JsonRpcRequest getJsonRpcRequest() {
    return httpRequest instanceof ProxyHttpServerRequestRequest ? ((ProxyHttpServerRequestRequest) httpRequest).getJsonRpcRequest() : jsonRpcRequest;
  }
}
