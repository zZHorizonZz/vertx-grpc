package io.vertx.jrpc.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.jrpc.mcp.proto.Tool;

import java.util.function.Function;

public interface ModelContextProtocolTool extends Function<JsonObject, Future<JsonObject>> {
  String id();

  Tool tool();

  ModelContextProtocolService service();
}
