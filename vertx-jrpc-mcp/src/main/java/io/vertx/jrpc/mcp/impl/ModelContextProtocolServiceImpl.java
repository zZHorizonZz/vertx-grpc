package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.Descriptors;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.handler.*;
import io.vertx.jrpc.mcp.proto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the ModelContextProtocolService.
 */
public class ModelContextProtocolServiceImpl implements ModelContextProtocolService {

  private static final ServiceName SERVICE_NAME = ServiceName.create("mcp.ModelContextProtocolService");
  // We'll set this in the constructor after we have access to the proto classes
  private static Descriptors.ServiceDescriptor SERVICE_DESCRIPTOR = ModelContextProtocolProto.getDescriptor().findServiceByName("ModelContextProtocolService");

  private final Vertx vertx;
  private final Map<String, String> capabilities = new ConcurrentHashMap<>();
  private final Map<String, String> activeRequests = new ConcurrentHashMap<>();
  private final List<Tool> availableTools = new ArrayList<>();
  private final List<Resource> availableResources = new ArrayList<>();
  private final List<Prompt> availablePrompts = new ArrayList<>();

  /**
   * Creates a new ModelContextProtocolServiceImpl.
   *
   * @param vertx the Vert.x instance
   */
  public ModelContextProtocolServiceImpl(Vertx vertx) {
    this.vertx = vertx;

    // Initialize with some default capabilities
    capabilities.put("protocol_version", "1.0");
    capabilities.put("supports_streaming", "false");

    // Add some example tools
    availableTools.add(Tool.newBuilder()
      .setId("tool1")
      .setName("Example Tool")
      .setDescription("An example tool for demonstration")
      .putParameters("param1", "string")
      .putParameters("param2", "number")
      .build());

    // Add some example resources
    availableResources.add(Resource.newBuilder()
      .setId("resource1")
      .setName("Example Resource")
      .setType("text")
      .setDescription("An example resource for demonstration")
      .build());

    // Add some example prompts
    availablePrompts.add(Prompt.newBuilder()
      .setId("prompt1")
      .setName("Example Prompt")
      .setDescription("An example prompt template for demonstration")
      .build());
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
    // Create service methods for each RPC method
    ServiceMethod<InitializeRequest, InitializeResponse> initializeMethod = ServiceMethod.server(
      SERVICE_NAME, "Initialize",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(InitializeRequest.newBuilder()));

    ServiceMethod<PingRequest, PingResponse> pingMethod = ServiceMethod.server(
      SERVICE_NAME, "Ping",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(PingRequest.newBuilder()));

    ServiceMethod<CancelRequest, CancelResponse> cancelMethod = ServiceMethod.server(
      SERVICE_NAME, "Cancel",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(CancelRequest.newBuilder()));

    ServiceMethod<ToolsListRequest, ToolsListResponse> toolsListMethod = ServiceMethod.server(
      SERVICE_NAME, "ToolsList",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(ToolsListRequest.newBuilder()));

    ServiceMethod<ToolsCallRequest, ToolsCallResponse> toolsCallMethod = ServiceMethod.server(
      SERVICE_NAME, "ToolsCall",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(ToolsCallRequest.newBuilder()));

    ServiceMethod<ResourcesListRequest, ResourcesListResponse> resourcesListMethod = ServiceMethod.server(
      SERVICE_NAME, "ResourcesList",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(ResourcesListRequest.newBuilder()));

    ServiceMethod<ResourcesReadRequest, ResourcesReadResponse> resourcesReadMethod = ServiceMethod.server(
      SERVICE_NAME, "ResourcesRead",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(ResourcesReadRequest.newBuilder()));

    ServiceMethod<ResourcesSubscribeRequest, ResourcesSubscribeResponse> resourcesSubscribeMethod = ServiceMethod.server(
      SERVICE_NAME, "ResourcesSubscribe",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(ResourcesSubscribeRequest.newBuilder()));

    ServiceMethod<ResourcesUnsubscribeRequest, ResourcesUnsubscribeResponse> resourcesUnsubscribeMethod = ServiceMethod.server(
      SERVICE_NAME, "ResourcesUnsubscribe",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(ResourcesUnsubscribeRequest.newBuilder()));

    ServiceMethod<PromptsListRequest, PromptsListResponse> promptsListMethod = ServiceMethod.server(
      SERVICE_NAME, "PromptsList",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(PromptsListRequest.newBuilder()));

    ServiceMethod<PromptsGetRequest, PromptsGetResponse> promptsGetMethod = ServiceMethod.server(
      SERVICE_NAME, "PromptsGet",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(PromptsGetRequest.newBuilder()));

    // Register all handlers with the server
    server.callHandler(initializeMethod, new InitializeHandler(server, this));
    server.callHandler(pingMethod, new PingHandler(server, this));
    server.callHandler(cancelMethod, new CancelHandler(server, this));
    server.callHandler(toolsListMethod, new ToolsListHandler(server, this));
    server.callHandler(toolsCallMethod, new ToolsCallHandler(server, this));
    server.callHandler(resourcesListMethod, new ResourcesListHandler(server, this));
    server.callHandler(resourcesReadMethod, new ResourcesReadHandler(server, this));
    server.callHandler(resourcesSubscribeMethod, new ResourcesSubscribeHandler(server, this));
    server.callHandler(resourcesUnsubscribeMethod, new ResourcesUnsubscribeHandler(server, this));
    server.callHandler(promptsListMethod, new PromptsListHandler(server, this));
    server.callHandler(promptsGetMethod, new PromptsGetHandler(server, this));
  }

  /**
   * Handles the initialize request.
   *
   * @param request the initialize request
   * @return a future with the initialize response
   */
  public Future<InitializeResponse> initialize(InitializeRequest request) {
    // Merge client capabilities with server capabilities
    capabilities.putAll(request.getCapabilitiesMap());

    // Create response with server information
    InitializeResponse response = InitializeResponse.newBuilder()
      .setServerName("Vert.x MCP Server")
      .setServerVersion("1.0.0")
      .putAllCapabilities(capabilities)
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
      .addAllTools(availableTools)
      .build());
  }

  /**
   * Calls a tool.
   *
   * @param request the tools call request
   * @return a future with the tools call response
   */
  public Future<ToolsCallResponse> toolsCall(ToolsCallRequest request) {
    String toolId = request.getToolId();
    Map<String, String> parameters = request.getParametersMap();

    // Check if tool exists
    boolean toolExists = availableTools.stream()
      .anyMatch(tool -> tool.getId().equals(toolId));

    if (!toolExists) {
      return Future.succeededFuture(ToolsCallResponse.newBuilder()
        .setSuccess(false)
        .setError("Tool not found: " + toolId)
        .build());
    }

    // In a real implementation, this would actually call the tool
    // For now, just return a mock response
    return Future.succeededFuture(ToolsCallResponse.newBuilder()
      .setSuccess(true)
      .setResult(new JsonObject()
        .put("toolId", toolId)
        .put("parameters", new JsonObject(new HashMap<>(parameters)))
        .put("result", "Tool executed successfully")
        .encode())
      .build());
  }

  /**
   * Lists available resources.
   *
   * @param request the resources list request
   * @return a future with the resources list response
   */
  public Future<ResourcesListResponse> resourcesList(ResourcesListRequest request) {
    String filter = request.getFilter();
    List<Resource> filteredResources = availableResources;

    // Apply filter if provided
    if (filter != null && !filter.isEmpty()) {
      filteredResources = availableResources.stream()
        .filter(resource -> resource.getName().contains(filter) ||
                           resource.getDescription().contains(filter))
        .collect(Collectors.toList());
    }

    return Future.succeededFuture(ResourcesListResponse.newBuilder()
      .addAllResources(filteredResources)
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
      .anyMatch(resource -> resource.getId().equals(resourceId));

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
      .anyMatch(resource -> resource.getId().equals(resourceId));

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
      .addAllPrompts(availablePrompts)
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
      .anyMatch(prompt -> prompt.getId().equals(promptId));

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
}
