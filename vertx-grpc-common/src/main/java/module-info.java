module io.vertx.grpc.common {

  requires static io.vertx.codegen.api;

  requires io.vertx.core;
  requires io.netty.common;
  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.compression;
  requires io.netty.transport;

  exports io.vertx.grpc.common;
  exports io.vertx.grpc.common.impl;

  provides io.vertx.core.spi.VertxServiceProvider with io.vertx.grpc.common.impl.GrpcRequestLocalRegistration;
}
