package io.vertx.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;

import java.util.function.Function;

public interface ModelContextProtocolTool extends Function<JsonObject, Future<ContentDataType>> {
  String id();

  String name();

  String title();

  String description();

  JsonSchema inputSchema();

  JsonSchema outputSchema();

  ModelContextProtocolServer service();
}
