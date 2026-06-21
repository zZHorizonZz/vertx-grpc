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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

public interface GrpcMessageDecoder<T> {

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
   * Create a decoder in JSON format decoding to instances of the {@code clazz} using
   * {@link Json#decodeValue(Buffer, Class)} (Jackson Databind is required).
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

  /**
   * Decodes a given gRPC message into an object of type {@code T}.
   *
   * @param msg the gRPC message to decode
   * @return the decoded message of type {@code T}
   * @throws CodecException if the decoding process fails
   */
  T decode(GrpcMessage msg) throws CodecException;

  /**
   * Determines whether the given {@link WireFormat} is supported by this decoder.
   *
   * @param format the wire format to check
   * @return true if the given wire format is accepted, false otherwise
   */
  boolean accepts(WireFormat format);

}
