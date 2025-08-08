package io.vertx.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

public interface ModelContextProtocolPromptProvider extends Function<JsonObject, Future<JsonObject>> {
  String id();

  ModelContextProtocolServer service();
}
