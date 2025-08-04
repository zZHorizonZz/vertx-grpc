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
package io.vertx.grpc.common;

import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public interface GrpcMessageDecoder<T> {

  /**
   * Create a decoder for a given protobuf {@link Parser}.
   *
   * @param messageOrBuilder the message or builder instance that returns decoded messages of type {@code <T>}
   * @return the message decoder
   */
  static <T> GrpcMessageDecoder<T> decoder(MessageOrBuilder messageOrBuilder) {
    Message dit = messageOrBuilder.getDefaultInstanceForType();
    Parser<T> parser = (Parser<T>) dit.getParserForType();
    return new GrpcMessageDecoder<>() {
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
              return (T) builder.build();
            } catch (InvalidProtocolBufferException e) {
              throw new CodecException(e);
            }
          case JSON_ARRAY:
            try {
              Object val = Json.decodeValue(msg.payload());

              if (val instanceof JsonArray) {
                JsonArray ja = (JsonArray) val;
                if (!ja.isEmpty()) {
                  Message.Builder builder = dit.toBuilder();
                  Descriptors.Descriptor ditfd = dit.getDescriptorForType();

                  JsonObject jo = new JsonObject();
                  if (ja.size() != ditfd.getFields().size()) {
                    throw new CodecException("The number of elements in the json array does not match the number of fields in the protobuf message");
                  }

                  for (int i = 0; i < ja.size(); i++) {
                    jo.put(ditfd.getFields().get(i).getName(), ja.getValue(i));
                  }

                  JsonFormat.parser().merge(jo.toBuffer().toString(StandardCharsets.UTF_8), builder);
                  return (T) builder.build();
                }
              }
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

  GrpcMessageDecoder<Buffer> IDENTITY = new GrpcMessageDecoder<>() {
    @Override
    public Buffer decode(GrpcMessage msg) throws CodecException {
      return msg.payload();
    }

    @Override
    public boolean accepts(WireFormat format) {
      return true;
    }
  };

  /**
   * Create a decoder for a given protobuf {@link Parser}.
   *
   * @param builder the supplier of a message builder
   * @return the message decoder
   */
  static <T> GrpcMessageDecoder<T> json(Supplier<Message.Builder> builder) {
    return new GrpcMessageDecoder<>() {
      @Override
      public T decode(GrpcMessage msg) throws CodecException {
        try {
          Message.Builder builderInstance = builder.get();
          JsonFormat.parser().merge(msg.payload().toString(StandardCharsets.UTF_8), builderInstance);
          return (T) builderInstance.build();
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

  /**
   * Create a decoder in JSON format decoding to instances of the {@code clazz} using {@link Json#decodeValue(Buffer, Class)} (Jackson Databind is required).
   *
   * @param clazz the java type to decode
   * @return a decoder that decodes messages to instance of {@code clazz} in JSON format.
   */
  static <T> GrpcMessageDecoder<T> json(Class<T> clazz) {
    return new GrpcMessageDecoder<>() {
      @Override
      public T decode(GrpcMessage msg) throws CodecException {
        if (!WireFormat.JSON.equals(msg.format())) {
          throw new CodecException("Was expecting a json message");
        }
        try {
          return Json.decodeValue(msg.payload(), clazz);
        } catch (DecodeException e) {
          throw new CodecException(e);
        }
      }

      @Override
      public boolean accepts(WireFormat format) {
        return format == WireFormat.JSON;
      }
    };
  }

  /**
   * A decoder in JSON format decoding to instances of {@link JsonObject}.
   */
  GrpcMessageDecoder<JsonObject> JSON_OBJECT = new GrpcMessageDecoder<JsonObject>() {
    @Override
    public JsonObject decode(GrpcMessage msg) throws CodecException {
      Object val = JSON_VALUE.decode(msg);
      if (val instanceof JsonObject) {
        return (JsonObject) val;
      } else {
        throw new CodecException("Was expecting an instance of JsonObject instead of " + val.getClass().getName());
      }
    }

    @Override
    public boolean accepts(WireFormat format) {
      return format == WireFormat.JSON;
    }
  };

  /**
   * A decoder in JSON format decoding arbitrary JSON values: {@link JsonObject}, {@link JsonArray} or string/number/boolean/null
   */
  GrpcMessageDecoder<Object> JSON_VALUE = new GrpcMessageDecoder<Object>() {
    @Override
    public Object decode(GrpcMessage msg) throws CodecException {
      if (!WireFormat.JSON.equals(msg.format())) {
        throw new CodecException("Was expecting a json message");
      }
      try {
        return Json.decodeValue(msg.payload());
      } catch (DecodeException e) {
        throw new CodecException(e);
      }
    }

    @Override
    public boolean accepts(WireFormat format) {
      return format == WireFormat.JSON;
    }
  };

  T decode(GrpcMessage msg) throws CodecException;

  boolean accepts(WireFormat format);

}
