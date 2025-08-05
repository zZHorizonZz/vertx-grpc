package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Structs;
import com.google.protobuf.util.Values;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.mcp.ModelContextProtocolPrompt;
import io.vertx.jrpc.mcp.ModelContextProtocolResource;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.ModelContextProtocolTool;
import io.vertx.jrpc.mcp.handler.*;
import io.vertx.jrpc.mcp.proto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the ModelContextProtocolService.
 */
public class ModelContextProtocolServiceImpl implements ModelContextProtocolService {

  private static final ServiceName SERVICE_NAME = ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService");
  // We'll set this in the constructor after we have access to the proto classes
  private static Descriptors.ServiceDescriptor SERVICE_DESCRIPTOR = ModelContextProtocolProto.getDescriptor().findServiceByName("ModelContextProtocolService");

  private final Vertx vertx;

  private final Map<String, String> activeRequests = new ConcurrentHashMap<>();

  private final List<ModelContextProtocolTool> availableTools = new ArrayList<>();
  private final List<ModelContextProtocolResource> availableResources = new ArrayList<>();
  private final List<ModelContextProtocolPrompt> availablePrompts = new ArrayList<>();

  private GrpcServer server;

  /**
   * Creates a new ModelContextProtocolServiceImpl.
   *
   * @param vertx the Vert.x instance
   */
  public ModelContextProtocolServiceImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public ServiceName name() {
    return SERVICE_NAME;
  }

  @Override
  public Descriptors.ServiceDescriptor descriptor() {
    return SERVICE_DESCRIPTOR;
  }

  @Override
  public void bind(GrpcServer server) {
    this.server = server;

    // Register all handlers with the server
    server.callHandler(InitializeHandler.SERVICE_METHOD, new InitializeHandler(server, this));
    server.callHandler(PingHandler.SERVICE_METHOD, new PingHandler(server, this));
    server.callHandler(CancelHandler.SERVICE_METHOD, new CancelHandler(server, this));
    server.callHandler(ToolsListHandler.SERVICE_METHOD, new ToolsListHandler(server, this));
    server.callHandler(ToolsCallHandler.SERVICE_METHOD, new ToolsCallHandler(server, this));
    server.callHandler(ResourcesListHandler.SERVICE_METHOD, new ResourcesListHandler(server, this));
    server.callHandler(ResourcesReadHandler.SERVICE_METHOD, new ResourcesReadHandler(server, this));
    server.callHandler(ResourcesSubscribeHandler.SERVICE_METHOD, new ResourcesSubscribeHandler(server, this));
    server.callHandler(ResourcesUnsubscribeHandler.SERVICE_METHOD, new ResourcesUnsubscribeHandler(server, this));
    server.callHandler(PromptsListHandler.SERVICE_METHOD, new PromptsListHandler(server, this));
    server.callHandler(PromptsGetHandler.SERVICE_METHOD, new PromptsGetHandler(server, this));
  }

  /**
   * Handles the initialize request.
   *
   * @param request the initialize request
   * @return a future with the initialize response
   */
  public Future<InitializeResponse> initialize(InitializeRequest request) {
    // Merge client capabilities with server capabilities
    //capabilities.putAll(request.getCapabilitiesMap());

    Struct.Builder capabilitiesBuilder = Struct.newBuilder();

    try {
      JsonFormat.parser().merge(getCapabilities().encode(), capabilitiesBuilder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    // Create response with server information
    InitializeResponse response = InitializeResponse.newBuilder()
      .setProtocolVersion(request.getProtocolVersion())
      .setServerInfo(InitializeResponse.ServerInfo.newBuilder()
        .setName("Vert.x MCP Server")
        .setVersion("1.0.0")
        .build()
      )
      .setCapabilities(capabilitiesBuilder.build())
      .build();

    return Future.succeededFuture(response);
  }

  /**
   * Handles the ping request.
   *
   * @param request the ping request
   * @return a future with the ping response
   */
  public Future<PingResponse> ping(PingRequest request) {
    return Future.succeededFuture(PingResponse.newBuilder()
      .setTimestamp(System.currentTimeMillis())
      .build());
  }

  /**
   * Handles the cancel request.
   *
   * @param request the cancel request
   * @return a future with the cancel response
   */
  public Future<CancelResponse> cancel(CancelRequest request) {
    String requestId = request.getRequestId();
    boolean success = activeRequests.remove(requestId) != null;

    return Future.succeededFuture(CancelResponse.newBuilder()
      .setSuccess(success)
      .build());
  }

  /**
   * Lists available tools.
   *
   * @param request the tools list request
   * @return a future with the tools list response
   */
  public Future<ToolsListResponse> toolsList(ToolsListRequest request) {
    return Future.succeededFuture(ToolsListResponse.newBuilder()
      .addAllTools(availableTools.stream().map(ModelContextProtocolTool::tool).collect(Collectors.toUnmodifiableSet()))
      .build());
  }

  /**
   * Calls a tool.
   *
   * @param request the tools call request
   * @return a future with the tools call response
   */
  public Future<ToolsCallResponse> toolsCall(ToolsCallRequest request) {
    String name = request.getName();
    JsonObject parameters;
    try {
      String arguments = JsonFormat.printer().print(request.getArguments());
      parameters = new JsonObject(arguments);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    // Check if tool exists
    Optional<ModelContextProtocolTool> toolExists = availableTools.stream().filter(tool -> tool.id().equals(name)).findAny();

    if (toolExists.isEmpty()) {
      throw new RuntimeException("Tool not found: " + name);
    }

    return toolExists.get().apply(parameters).map(result -> {
      Struct.Builder content = Struct.newBuilder();
      try {
        JsonFormat.parser().merge(result.encode(), content);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }

      return ToolsCallResponse.newBuilder()
        .addContent(Structs.of("type", Values.of("text"), "text", Values.of(result.encode())))
        .setStructuredContent(content)
        .build();
    });
  }

  /**
   * Lists available resources.
   *
   * @param request the resources list request
   * @return a future with the resources list response
   */
  public Future<ResourcesListResponse> resourcesList(ResourcesListRequest request) {
    String filter = request.getFilter();
    List<ModelContextProtocolResource> filteredResources = availableResources;

    // Apply filter if provided
    if (!filter.isEmpty()) {
      filteredResources = availableResources.stream()
        .filter(resource -> resource.resource().getName().contains(filter) ||
          resource.resource().getDescription().contains(filter))
        .collect(Collectors.toList());
    }

    return Future.succeededFuture(ResourcesListResponse.newBuilder()
      .addAllResources(filteredResources.stream().map(ModelContextProtocolResource::resource).collect(Collectors.toUnmodifiableSet()))
      .build());
  }

  /**
   * Reads a resource.
   *
   * @param request the resources read request
   * @return a future with the resources read response
   */
  public Future<ResourcesReadResponse> resourcesRead(ResourcesReadRequest request) {
    String resourceId = request.getResourceId();

    // Check if resource exists
    boolean resourceExists = availableResources.stream()
      .anyMatch(resource -> resource.id().equals(resourceId));

    if (!resourceExists) {
      return Future.failedFuture("Resource not found: " + resourceId);
    }

    // In a real implementation, this would actually read the resource
    // For now, just return a mock response
    return Future.succeededFuture(ResourcesReadResponse.newBuilder()
      .setContent("This is the content of resource " + resourceId)
      .setContentType("text/plain")
      .build());
  }

  /**
   * Subscribes to a resource.
   *
   * @param request the resources subscribe request
   * @return a future with the resources subscribe response
   */
  public Future<ResourcesSubscribeResponse> resourcesSubscribe(ResourcesSubscribeRequest request) {
    String resourceId = request.getResourceId();

    // Check if resource exists
    boolean resourceExists = availableResources.stream()
      .anyMatch(resource -> resource.id().equals(resourceId));

    if (!resourceExists) {
      return Future.succeededFuture(ResourcesSubscribeResponse.newBuilder()
        .setSuccess(false)
        .build());
    }

    // Generate a subscription ID
    String subscriptionId = "sub_" + System.currentTimeMillis();

    return Future.succeededFuture(ResourcesSubscribeResponse.newBuilder()
      .setSuccess(true)
      .setSubscriptionId(subscriptionId)
      .build());
  }

  /**
   * Unsubscribes from a resource.
   *
   * @param request the resources unsubscribe request
   * @return a future with the resources unsubscribe response
   */
  public Future<ResourcesUnsubscribeResponse> resourcesUnsubscribe(ResourcesUnsubscribeRequest request) {
    String subscriptionId = request.getSubscriptionId();

    // In a real implementation, this would actually unsubscribe
    // For now, just return a mock response
    return Future.succeededFuture(ResourcesUnsubscribeResponse.newBuilder()
      .setSuccess(true)
      .build());
  }

  /**
   * Lists available prompts.
   *
   * @param request the prompts list request
   * @return a future with the prompts list response
   */
  public Future<PromptsListResponse> promptsList(PromptsListRequest request) {
    return Future.succeededFuture(PromptsListResponse.newBuilder()
      .addAllPrompts(availablePrompts.stream().map(ModelContextProtocolPrompt::prompt).collect(Collectors.toUnmodifiableSet()))
      .build());
  }

  /**
   * Gets a prompt.
   *
   * @param request the prompts get request
   * @return a future with the prompts get response
   */
  public Future<PromptsGetResponse> promptsGet(PromptsGetRequest request) {
    String promptId = request.getPromptId();

    // Check if prompt exists
    boolean promptExists = availablePrompts.stream()
      .anyMatch(prompt -> prompt.id().equals(promptId));

    if (!promptExists) {
      return Future.failedFuture("Prompt not found: " + promptId);
    }

    // In a real implementation, this would actually get the prompt
    // For now, just return a mock response
    return Future.succeededFuture(PromptsGetResponse.newBuilder()
      .setContent("This is the content of prompt " + promptId)
      .putMetadata("author", "Vert.x")
      .putMetadata("version", "1.0")
      .build());
  }

  /**
   * Adds a tool to the available tools list.
   *
   * @param tool the tool to add
   */
  public void addTool(ModelContextProtocolTool tool) {
    availableTools.add(tool);
  }

  /**
   * Adds a resource to the available resources list.
   *
   * @param resource the resource to add
   */
  public void addResource(ModelContextProtocolResource resource) {
    availableResources.add(resource);
  }

  /**
   * Adds a prompt to the available prompts list.
   *
   * @param prompt the prompt to add
   */
  public void addPrompt(ModelContextProtocolPrompt prompt) {
    availablePrompts.add(prompt);
  }

  public JsonObject getCapabilities() {
    JsonObject capabilities = new JsonObject();

    //capabilities.put("protocol_version", "1.0");
    //capabilities.put("supports_streaming", "false");

    capabilities.put("completions", new JsonObject());
    capabilities.put("tools", new JsonObject().put("listChanged", true));
    capabilities.put("prompts", new JsonObject().put("listChanged", true));
    capabilities.put("resources", new JsonObject().put("listChanged", true));

    return capabilities;
  }
}
