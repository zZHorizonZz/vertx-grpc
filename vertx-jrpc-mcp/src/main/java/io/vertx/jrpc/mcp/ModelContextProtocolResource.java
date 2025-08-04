package io.vertx.jrpc.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.jrpc.mcp.proto.Resource;

import java.util.function.Function;

public interface ModelContextProtocolResource extends Function<JsonObject, Future<JsonObject>> {
  String id();

  Resource resource();

  ModelContextProtocolService service();
}
