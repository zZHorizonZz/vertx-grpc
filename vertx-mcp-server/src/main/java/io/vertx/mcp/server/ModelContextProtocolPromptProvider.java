package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

public interface ModelContextProtocolPromptProvider extends Function<JsonObject, Future<JsonObject>> {
  String id();
}
