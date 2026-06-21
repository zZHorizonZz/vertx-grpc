package io.vertx.grpc.common;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public interface GrpcMessageEncoder<T> {

  GrpcMessageEncoder<Buffer> IDENTITY = new GrpcMessageEncoder<>() {
    @Override
    public GrpcMessage encode(Buffer msg, WireFormat format) throws CodecException {
      return GrpcMessage.message("identity", format, msg);
    }
    @Override
    public boolean accepts(WireFormat format) {
      return true;
    }
  };

  /**
   * An encoder in JSON format encoding {@link JsonObject} instances.
   */
  GrpcMessageEncoder<JsonObject> JSON_OBJECT = new GrpcMessageEncoder<>() {
    @Override
    public GrpcMessage encode(JsonObject msg, WireFormat format) throws CodecException {
      return GrpcMessage.message("identity", WireFormat.JSON, msg == null ? Buffer.buffer("null") : msg.toBuffer());
    }
    @Override
    public boolean accepts(WireFormat format) {
      return format == WireFormat.JSON;
    }
  };

  GrpcMessage encode(T msg, WireFormat format) throws CodecException;

  boolean accepts(WireFormat format);

}
