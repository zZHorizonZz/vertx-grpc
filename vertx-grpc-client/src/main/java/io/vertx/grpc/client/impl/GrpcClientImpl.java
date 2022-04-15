/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.client.impl;

import io.grpc.MethodDescriptor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.common.GrpcMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcClientImpl implements GrpcClient {

  private final Vertx vertx;
  private HttpClient client;

  public GrpcClientImpl(HttpClientOptions options, Vertx vertx) {
    this.vertx = vertx;
    this.client = vertx.createHttpClient(new HttpClientOptions(options)
      .setProtocolVersion(HttpVersion.HTTP_2));
  }

  public GrpcClientImpl(Vertx vertx) {
    this(new HttpClientOptions().setHttp2ClearTextUpgrade(false), vertx);
  }

  @Override public Future<GrpcClientRequest<GrpcMessage, GrpcMessage>> request(SocketAddress server) {
    RequestOptions options = new RequestOptions()
      .setMethod(HttpMethod.POST)
      .setServer(server);
    return client.request(options)
      .map(request -> new GrpcClientRequestImpl<>(request, Function.identity(), Function.identity()));
  }

  @Override public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(SocketAddress server, MethodDescriptor<Req, Resp> method) {
    Function<Req, GrpcMessage> encoder = msg -> {
      Buffer encoded = Buffer.buffer();
      InputStream stream = method.streamRequest(msg);
      byte[] tmp = new byte[256];
      int i;
      try {
        while ((i = stream.read(tmp)) != -1) {
          encoded.appendBytes(tmp, 0, i);
        }
      } catch (IOException e) {
        throw new VertxException(e);
      }
      return GrpcMessage.message(encoded);
    };
    Function<GrpcMessage, Resp> decoder = msg -> {
      ByteArrayInputStream in = new ByteArrayInputStream(msg.payload().getBytes());
      return method.parseResponse(in);
    };
    return request(server, decoder, encoder, method);
  }

  @Override
  public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(SocketAddress server, Function<GrpcMessage, Resp> messageDecoder, Function<Req, GrpcMessage> messageEncoder, MethodDescriptor<Req, Resp> method) {
    RequestOptions options = new RequestOptions()
      .setMethod(HttpMethod.POST)
      .setServer(server);
    return client.request(options)
      .map(request -> {
        GrpcClientRequestImpl<Req, Resp> call = new GrpcClientRequestImpl<>(request, messageEncoder, messageDecoder);
        call.fullMethodName(method.getFullMethodName());
        return call;
      });
  }
}
