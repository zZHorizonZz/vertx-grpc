/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.WireFormat;

/**
 * Protobuf message encoders for the Vert.x gRPC SPI. Extracted from {@code vertx-grpc-common} so
 * that the core stays free of any {@code com.google.protobuf} dependency.
 */
public final class ProtobufMessageEncoder {

  private ProtobufMessageEncoder() {
  }

  /**
   * Create an encoder for arbitrary message extending {@link MessageLite}.
   *
   * @return the message encoder
   */
  public static <T extends MessageLite> GrpcMessageEncoder<T> encoder() {
    return new GrpcMessageEncoder<T>() {
      @Override
      public GrpcMessage encode(T msg, WireFormat format) throws CodecException {
        switch (format) {
          case PROTOBUF:
            return GrpcMessage.message("identity", Buffer.buffer(msg.toByteArray()));
          case JSON:
            if (msg instanceof MessageOrBuilder) {
              try {
                String res = JsonFormat.printer().print((MessageOrBuilder) msg);
                return GrpcMessage.message("identity", WireFormat.JSON, Buffer.buffer(res));
              } catch (InvalidProtocolBufferException e) {
                throw new CodecException(e);
              }
            }
            return GrpcMessage.message("identity", WireFormat.JSON, Json.encodeToBuffer(msg));
          default:
            throw new IllegalArgumentException("Invalid wire format: " + format);
        }
      }

      @Override
      public boolean accepts(WireFormat format) {
        return true;
      }
    };
  }

  /**
   * Create and return an encoder in JSON format encoding instances of {@code MessageOrBuilder} using
   * the protobuf-java-util library otherwise using {@link Json#encodeToBuffer(Object)}.
   *
   * @return an encoder in JSON format encoding instances of {@code <T>}.
   */
  public static <T> GrpcMessageEncoder<T> json() {
    return new GrpcMessageEncoder<T>() {
      @Override
      public GrpcMessage encode(T msg, WireFormat format) throws CodecException {
        if (msg instanceof MessageOrBuilder) {
          try {
            String res = JsonFormat.printer().print((MessageOrBuilder) msg);
            return GrpcMessage.message("identity", WireFormat.JSON, Buffer.buffer(res));
          } catch (InvalidProtocolBufferException e) {
            throw new CodecException(e);
          }
        }
        return GrpcMessage.message("identity", WireFormat.JSON, Json.encodeToBuffer(msg));
      }

      @Override
      public boolean accepts(WireFormat format) {
        return format == WireFormat.JSON;
      }
    };
  }
}
