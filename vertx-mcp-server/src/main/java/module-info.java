module io.vertx.mcp.server {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;

  requires io.vertx.mcp.jrpc;
  requires io.vertx.mcp;

  requires io.vertx.codegen.json;
  requires io.vertx.core;

  exports io.vertx.mcp.server;
}
