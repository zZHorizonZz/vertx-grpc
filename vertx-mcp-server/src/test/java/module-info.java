open module io.vertx.tests.mcp.bridge.grpc {
  requires junit;
  requires io.vertx.testing.unit;
  requires io.vertx.tests.server;

  requires io.vertx.core;
  requires io.vertx.grpc.common;
  requires io.vertx.grpc.server;

  requires io.vertx.mcp.bridge.grpc;
  requires io.vertx.mcp;
  requires io.vertx.mcp.jrpc;

  requires com.google.protobuf;

  exports io.vertx.tests.mcp.bridge.grpc;
}
