module io.vertx.mcp.bridge.grpc {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;
  requires io.vertx.grpc.server;

  requires io.vertx.mcp.jrpc;
  requires io.vertx.mcp;

  requires com.google.protobuf;
  requires io.vertx.grpc.common;
  requires io.netty.codec;
  requires io.vertx.grpc.transcoding;
  requires proto.google.common.protos;
  requires com.google.protobuf.util;
  requires io.grpc;
  requires io.grpc.stub;
  requires io.grpc.protobuf;
  requires io.vertx.codegen.json;
  requires io.vertx.core;

  exports io.vertx.mcp.bridge.grpc;

  provides io.vertx.grpc.server.impl.GrpcHttpInvoker with io.vertx.mcp.bridge.grpc.impl.ModelContextProtocolHttpInvoker;
}
