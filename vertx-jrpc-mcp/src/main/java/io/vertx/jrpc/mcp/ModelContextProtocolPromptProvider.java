package io.vertx.jrpc.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.jrpc.mcp.proto.Prompt;

import java.util.function.Function;

public interface ModelContextProtocolPromptProvider extends Function<JsonObject, Future<JsonObject>> {
  String id();

  Prompt prompt();

  ModelContextProtocolService service();
}
