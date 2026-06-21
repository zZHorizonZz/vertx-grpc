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

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.util.JsonFormat;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.WireFormat;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Protobuf message decoders for the Vert.x gRPC SPI. Extracted from {@code vertx-grpc-common} so
 * that the core stays free of any {@code com.google.protobuf} dependency.
 * <p>
 * A {@code ProtobufMessageDecoder} additionally exposes the protobuf {@link Descriptors.Descriptor}
 * of the message it decodes, which descriptor-driven features (transcoding, reflection) rely on.
 */
public interface ProtobufMessageDecoder<T> extends GrpcMessageDecoder<T> {

  /**
   * @return the protobuf message descriptor of the decoded message
   */
  Descriptors.Descriptor messageDescriptor();

  /**
   * Create a decoder for a given protobuf message.
   *
   * @param messageOrBuilder the message or builder instance that returns decoded messages of type {@code <T>}
   * @return the message decoder
   */
  static <T> ProtobufMessageDecoder<T> decoder(MessageOrBuilder messageOrBuilder) {
    Message dit = messageOrBuilder.getDefaultInstanceForType();
    @SuppressWarnings("unchecked")
    Parser<T> parser = (Parser<T>) dit.getParserForType();
    return new ProtobufMessageDecoder<T>() {
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

      @Override
      public Descriptors.Descriptor messageDescriptor() {
        return dit.getDescriptorForType();
      }
    };
  }

  /**
   * Create a JSON decoder for a given protobuf message builder.
   *
   * @param builder the supplier of a message builder
   * @return the message decoder
   */
  static <T> GrpcMessageDecoder<T> json(Supplier<Message.Builder> builder) {
    return new GrpcMessageDecoder<T>() {
      @Override
      public T decode(GrpcMessage msg) throws CodecException {
        try {
          Message.Builder builderInstance = builder.get();
          JsonFormat.parser().merge(msg.payload().toString(StandardCharsets.UTF_8), builderInstance);
          @SuppressWarnings("unchecked")
          T result = (T) builderInstance.build();
          return result;
        } catch (InvalidProtocolBufferException e) {
          throw new CodecException(e);
        }
      }

      @Override
      public boolean accepts(WireFormat format) {
        return format == WireFormat.JSON;
      }
    };
  }
}
