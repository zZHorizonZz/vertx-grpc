module io.vertx.grpc.server {
  requires io.vertx.core.logging;
  requires transitive io.vertx.grpc.common;
  requires static io.vertx.docgen;
  requires static io.vertx.codegen.json;
  requires io.vertx.codegen.api;
  requires io.netty.codec;
  requires io.netty.buffer;
  exports io.vertx.grpc.server;
  exports io.vertx.grpc.server.impl to io.vertx.grpc.transcoding;
}
