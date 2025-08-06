package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.Descriptors;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.ServiceName;
import io.vertx.jrpc.mcp.*;
import io.vertx.jrpc.mcp.proto.ModelContextProtocolProto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the ModelContextProtocolService.
 */
public class ModelContextProtocolServiceImpl implements ModelContextProtocolService {

  private static final ServiceName SERVICE_NAME = ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService");
  private static final Descriptors.ServiceDescriptor SERVICE_DESCRIPTOR = ModelContextProtocolProto.getDescriptor().findServiceByName("ModelContextProtocolService");

  private final Vertx vertx;

  private final Map<Integer, Promise<?>> activeRequests = new ConcurrentHashMap<>();

  private final List<ModelContextProtocolTool> availableTools = new ArrayList<>();
  private final List<ModelContextProtocolResourceProvider> availableResourceProviders = new ArrayList<>();
  private final List<ModelContextProtocolPromptProvider> availablePromptProviders = new ArrayList<>();

  /**
   * Creates a new ModelContextProtocolServiceImpl.
   *
   * @param vertx the Vert.x instance
   */
  public ModelContextProtocolServiceImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public String getProtocolVersion() {
    return "2025-06-18";
  }

  @Override
  public String getServerVersion() {
    return "1.0";
  }

  @Override
  public String getServerName() {
    return "Vert.x MCP Server";
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
    availableTools.add(tool);
  }

  @Override
  public void registerResourceProvider(ModelContextProtocolResourceProvider resource) {
    availableResourceProviders.add(resource);
  }

  @Override
  public void registerPromptProvider(ModelContextProtocolPromptProvider prompt) {
    availablePromptProviders.add(prompt);
  }

  @Override
  public List<ModelContextProtocolTool> toolsList() {
    return Collections.unmodifiableList(availableTools);
  }

  @Override
  public List<ModelContextProtocolResourceProvider> resourcesList() {
    return Collections.unmodifiableList(availableResourceProviders);
  }

  @Override
  public List<ModelContextProtocolPromptProvider> promptsList() {
    return Collections.unmodifiableList(availablePromptProviders);
  }

  @Override
  public Future<ModelContextProtocolDataType> executeTool(String tool, JsonObject parameters) {
    Optional<ModelContextProtocolTool> toolExists = availableTools.stream().filter(t -> t.id().equals(tool)).findFirst();

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
