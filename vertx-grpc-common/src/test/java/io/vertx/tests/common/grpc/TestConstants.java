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
package io.vertx.tests.common.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.common.WireFormat;

import java.nio.charset.StandardCharsets;

/**
 * Shared test fixtures for the gRPC test suites.
 * <p>
 * The protobuf encoders/decoders are inlined here (using protobuf-java directly) rather than reusing
 * {@code vertx-grpc-protobuf}: this fixture lives in {@code vertx-grpc-common}'s test sources, and
 * {@code vertx-grpc-protobuf} depends on {@code vertx-grpc-common}, so depending on it (even in test
 * scope) would create a Maven reactor cycle. The core itself no longer carries a protobuf codec.
 */
public final class TestConstants {

  public static final ServiceName TEST_SERVICE = ServiceName.create("io.vertx.tests.common.grpc.tests.TestService");
  public static final GrpcMessageEncoder<Empty> EMPTY_ENC = encoder();
  public static final GrpcMessageDecoder<Empty> EMPTY_DEC = decoder(Empty.newBuilder());
  public static final GrpcMessageEncoder<Request> REQUEST_ENC = encoder();
  public static final GrpcMessageDecoder<Request> REQUEST_DEC = decoder(Request.newBuilder());
  public static final GrpcMessageEncoder<Reply> REPLY_ENC = encoder();
  public static final GrpcMessageDecoder<Reply> REPLY_DEC = decoder(Reply.newBuilder());

  private static <T extends MessageLite> GrpcMessageEncoder<T> encoder() {
    return new GrpcMessageEncoder<T>() {
      @Override
      public GrpcMessage encode(T msg, WireFormat format) throws CodecException {
        switch (format) {
          case PROTOBUF:
            return GrpcMessage.message("identity", Buffer.buffer(msg.toByteArray()));
          case JSON:
            if (msg instanceof MessageOrBuilder) {
              try {
                return GrpcMessage.message("identity", WireFormat.JSON, Buffer.buffer(JsonFormat.printer().print((MessageOrBuilder) msg)));
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

  private static <T> GrpcMessageDecoder<T> decoder(MessageOrBuilder messageOrBuilder) {
    Message dit = messageOrBuilder.getDefaultInstanceForType();
    @SuppressWarnings("unchecked")
    Parser<T> parser = (Parser<T>) dit.getParserForType();
    return new GrpcMessageDecoder<T>() {
      @Override
      public T decode(GrpcMessage msg) throws CodecException {
        switch (msg.format()) {
          case PROTOBUF:
            try {
              return parser.parseFrom(msg.payload().getBytes());
            } catch (InvalidProtocolBufferException e) {
              throw new CodecException(e);
            }
          case JSON:
            try {
              Message.Builder builder = dit.toBuilder();
              JsonFormat.parser().merge(msg.payload().toString(StandardCharsets.UTF_8), builder);
              @SuppressWarnings("unchecked")
              T result = (T) builder.build();
              return result;
            } catch (InvalidProtocolBufferException e) {
              throw new CodecException(e);
            }
          default:
            throw new IllegalArgumentException("Invalid wire format: " + msg.format());
        }
      }

      @Override
      public boolean accepts(WireFormat format) {
        return true;
      }
    };
  }

  private TestConstants() {
  }
}
