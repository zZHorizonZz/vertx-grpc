package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.*;
import io.vertx.mcp.server.ModelContextProtocolPromptProvider;
import io.vertx.mcp.server.ModelContextProtocolResourceProvider;
import io.vertx.mcp.server.ModelContextProtocolServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the ModelContextProtocolServer.
 */
public class ModelContextProtocolServerImpl implements ModelContextProtocolServer {

  private final ModelContextProtocolOptions options;

  private final Map<Integer, Promise<?>> activeRequests = new ConcurrentHashMap<>();

  private final List<ModelContextProtocolTool> tools = new ArrayList<>();
  private final List<ModelContextProtocolResourceTemplate> resourcesTemplates = new ArrayList<>();
  private final List<ModelContextProtocolResourceProvider> resourceProviders = new ArrayList<>();
  private final List<ModelContextProtocolPromptProvider> promptProviders = new ArrayList<>();

  public ModelContextProtocolServerImpl(ModelContextProtocolOptions options) {
    this.options = options;
  }

  @Override
  public String getProtocolVersion() {
    return options.getProtocolVersion();
  }

  @Override
  public String getServerVersion() {
    return options.getServerVersion();
  }

  @Override
  public String getServerName() {
    return options.getServerName();
  }

  @Override
  public JsonObject getCapabilities() {
    JsonObject capabilities = new JsonObject();

    capabilities.put("completions", new JsonObject());
    capabilities.put("tools", new JsonObject().put("listChanged", true));
    capabilities.put("prompts", new JsonObject().put("listChanged", true));
    capabilities.put("resources", new JsonObject().put("listChanged", true));

    return capabilities;
  }

  @Override
  public void registerTool(ModelContextProtocolTool tool) {
    tools.add(tool);
  }

  @Override
  public void registerResourceTemplate(ModelContextProtocolResourceTemplate template) {
    resourcesTemplates.add(template);
  }

  @Override
  public void registerResourceProvider(ModelContextProtocolResourceProvider resource) {
    resourceProviders.add(resource);
  }

  @Override
  public void registerPromptProvider(ModelContextProtocolPromptProvider prompt) {
    promptProviders.add(prompt);
  }

  @Override
  public List<ModelContextProtocolTool> toolsList() {
    return Collections.unmodifiableList(tools);
  }

  @Override
  public List<ModelContextProtocolResourceTemplate> resourcesTemplatesList() {
    return Collections.unmodifiableList(resourcesTemplates);
  }

  @Override
  public List<ModelContextProtocolResourceProvider> resourcesList() {
    return Collections.unmodifiableList(resourceProviders);
  }

  @Override
  public List<ModelContextProtocolPromptProvider> promptsList() {
    return Collections.unmodifiableList(promptProviders);
  }

  @Override
  public Future<ContentDataType> executeTool(String tool, JsonObject parameters) {
    Optional<ModelContextProtocolTool> toolExists = tools.stream().filter(t -> t.id().equals(tool)).findFirst();

    if (toolExists.isEmpty()) {
      throw new RuntimeException("Tool with id " + tool + " not found");
    }

    return toolExists.get().apply(parameters);
  }

  @Override
  public Future<JsonObject> executeResource(String resource, JsonObject parameters) {
    return null;
  }

  @Override
  public Future<JsonObject> executePrompt(String prompt, JsonObject parameters) {
    return null;
  }

  @Override
  public boolean cancelRequest(Integer requestId) {
    Promise<?> promise = activeRequests.remove(requestId);
    if (promise != null) {
      return promise.tryFail("Cancelled by user");
    }
    return false;
  }
}
