open module io.vertx.tests.reflection {
  requires com.google.common;
  requires com.google.protobuf;
  requires com.google.protobuf.util;
  requires io.grpc;
  requires io.grpc.stub;
  requires io.grpc.util;
  requires io.grpc.protobuf;
  requires io.vertx.core;
  requires io.vertx.grpc.common;
  requires io.vertx.grpc.server;
  requires io.vertx.grpc.reflection;
  requires io.vertx.testing.unit;
  requires io.vertx.tests.common;
  requires io.vertx.tests.server;
  requires junit;
  requires testcontainers;
  exports io.vertx.tests.reflection.grpc;
}
