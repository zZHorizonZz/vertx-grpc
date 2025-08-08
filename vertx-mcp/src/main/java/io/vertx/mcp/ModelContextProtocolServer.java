package io.vertx.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.impl.ModelContextProtocolServerImpl;

import java.util.List;

public interface ModelContextProtocolServer {

  static ModelContextProtocolServer create(ModelContextProtocolOptions options) {
    return new ModelContextProtocolServerImpl(options);
  }

  String getProtocolVersion();

  String getServerName();

  String getServerVersion();

  JsonObject getCapabilities();

  void registerTool(ModelContextProtocolTool tool);

  void registerResourceTemplate(ModelContextProtocolResourceTemplate template);

  void registerResourceProvider(ModelContextProtocolResourceProvider resource);

  void registerPromptProvider(ModelContextProtocolPromptProvider prompt);

  List<ModelContextProtocolTool> toolsList();

  List<ModelContextProtocolResourceTemplate> resourcesTemplatesList();

  List<ModelContextProtocolResourceProvider> resourcesList();

  List<ModelContextProtocolPromptProvider> promptsList();

  Future<ContentDataType> executeTool(String tool, JsonObject parameters);

  Future<JsonObject> executeResource(String resource, JsonObject parameters);

  Future<JsonObject> executePrompt(String prompt, JsonObject parameters);

  boolean cancelRequest(Integer requestId);
}
