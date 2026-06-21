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
package io.vertx.grpc.schema;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.common.WireFormat;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.protobuf.ProtoWire;

/**
 * Hooks the schema-driven Protobuf codec ({@link ProtoWire}) up to the Vert.x gRPC SPI: it adapts
 * a general {@link ObjectSchema} into the {@link GrpcMessageEncoder}/{@link GrpcMessageDecoder}
 * the gRPC client and server already speak, and bundles them into {@link ServiceMethod}s.
 * <p>
 * Messages are exchanged as {@link JsonObject}. On the wire they are encoded as Protobuf
 * (interoperable with regular protobuf/gRPC peers) or as JSON, selected by the {@link WireFormat}.
 */
public final class ProtoSchema {

  private ProtoSchema() {
  }

  /**
   * An encoder that serializes a {@link JsonObject} according to {@code schema}.
   */
  public static GrpcMessageEncoder<JsonObject> encoder(ObjectSchema schema) {
    return new GrpcMessageEncoder<JsonObject>() {
      @Override
      public GrpcMessage encode(JsonObject msg, WireFormat format) throws CodecException {
        switch (format) {
          case PROTOBUF:
            return GrpcMessage.message("identity", WireFormat.PROTOBUF, Buffer.buffer(ProtoWire.write(schema, msg)));
          case JSON:
            return GrpcMessage.message("identity", WireFormat.JSON, msg == null ? Buffer.buffer("null") : msg.toBuffer());
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
   * A decoder that deserializes a {@link JsonObject} according to {@code schema}.
   */
  public static GrpcMessageDecoder<JsonObject> decoder(ObjectSchema schema) {
    return new GrpcMessageDecoder<JsonObject>() {
      @Override
      public JsonObject decode(GrpcMessage msg) throws CodecException {
        switch (msg.format()) {
          case PROTOBUF:
            return ProtoWire.read(schema, msg.payload().getBytes());
          case JSON:
            return new JsonObject(msg.payload());
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

  /**
   * A server-side {@link ServiceMethod}: decodes the request and encodes the response from their schemas.
   */
  public static ServiceMethod<JsonObject, JsonObject> server(ServiceName serviceName, String methodName, ObjectSchema requestSchema, ObjectSchema responseSchema) {
    return ServiceMethod.server(serviceName, methodName, encoder(responseSchema), decoder(requestSchema));
  }

  /**
   * A client-side {@link ServiceMethod}: encodes the request and decodes the response from their schemas.
   */
  public static ServiceMethod<JsonObject, JsonObject> client(ServiceName serviceName, String methodName, ObjectSchema requestSchema, ObjectSchema responseSchema) {
    return ServiceMethod.client(serviceName, methodName, encoder(requestSchema), decoder(responseSchema));
  }
}
