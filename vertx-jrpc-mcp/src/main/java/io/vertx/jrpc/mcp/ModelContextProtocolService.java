package io.vertx.jrpc.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ModelContextProtocolService {

  String getProtocolVersion();

  String getServerName();

  String getServerVersion();

  JsonObject getCapabilities();

  void registerTool(ModelContextProtocolTool tool);

  void registerResourceProvider(ModelContextProtocolResourceProvider resource);

  void registerPromptProvider(ModelContextProtocolPromptProvider prompt);

  List<ModelContextProtocolTool> toolsList();

  List<ModelContextProtocolResourceProvider> resourcesList();

  List<ModelContextProtocolPromptProvider> promptsList();

  Future<ModelContextProtocolDataType> executeTool(String tool, JsonObject parameters);

  Future<JsonObject> executeResource(String resource, JsonObject parameters);

  Future<JsonObject> executePrompt(String prompt, JsonObject parameters);

  boolean cancelRequest(Integer requestId);
}
