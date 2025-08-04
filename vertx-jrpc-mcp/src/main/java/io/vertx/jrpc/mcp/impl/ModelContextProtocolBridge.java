package io.vertx.jrpc.mcp.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.ModelContextProtocolTool;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;
import io.vertx.jrpc.transcoding.model.JsonRpcResponse;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bridge that connects MCP service with JSON-RPC transcoding and HTTP client execution.
 */
public class ModelContextProtocolBridge {

  private final Vertx vertx;
  private final ModelContextProtocolService mcpService;
  private GrpcServer grpcServer;
  private HttpClient httpClient;
  private int port;
  private final AtomicInteger requestIdCounter = new AtomicInteger(1);

  public ModelContextProtocolBridge(Vertx vertx, ModelContextProtocolService mcpService) {
    this.vertx = vertx;
    this.mcpService = mcpService;
  }

  /**
   * Registers the MCP service methods with the gRPC server for JSON-RPC transcoding.
   */
  public ModelContextProtocolBridge bind(GrpcServer grpcServer) {
    this.grpcServer = grpcServer;

    // Bind the MCP service to the gRPC server
    mcpService.bind(grpcServer);

    grpcServer.services().forEach(service -> service.methodDescriptors().forEach(methodDescriptor -> {
      ModelContextProtocolTool httpTool = createHttpClientTool(
        service.name().fullyQualifiedName(),
        methodDescriptor.getName(),
        methodDescriptor.getName() + " via HTTP client"
      );
      mcpService.addTool(httpTool);
    }));

    return this;
  }

  /**
   * Sets up HTTP client for self-calling mechanism.
   */
  public ModelContextProtocolBridge withHttpClient(int port) {
    this.port = port;
    this.httpClient = vertx.createHttpClient(new HttpClientOptions()
      .setDefaultHost("localhost")
      .setDefaultPort(port));
    return this;
  }

  /**
   * Creates a tool that executes methods via HTTP client calls to itself.
   */
  public ModelContextProtocolTool createHttpClientTool(String toolId, String methodName, String description) {
    return new HttpClientTool(toolId, methodName, description);
  }

  /**
   * Tool implementation that uses HTTP client to call MCP methods via JSON-RPC.
   */
  private class HttpClientTool implements ModelContextProtocolTool {
    private final String toolId;
    private final String methodName;
    private final String description;

    public HttpClientTool(String toolId, String methodName, String description) {
      this.toolId = toolId;
      this.methodName = methodName;
      this.description = description;
    }

    @Override
    public String id() {
      return toolId;
    }

    @Override
    public io.vertx.jrpc.mcp.proto.Tool tool() {
      return io.vertx.jrpc.mcp.proto.Tool.newBuilder()
        .setName(toolId)
        .setDescription(description)
        .build();
    }

    @Override
    public ModelContextProtocolService service() {
      return mcpService;
    }

    @Override
    public Future<JsonObject> apply(JsonObject parameters) {
      if (httpClient == null) {
        return Future.failedFuture("HTTP client not configured. Call withHttpClient() first.");
      }

      // Create JSON-RPC request
      JsonRpcRequest jsonRpcRequest = new JsonRpcRequest(
        methodName,
        parameters,
        String.valueOf(requestIdCounter.getAndIncrement())
      );

      // Make HTTP call to self
      return httpClient.request(HttpMethod.POST, "/" + toolId)
        .compose(request -> {
          request.putHeader("Content-Type", "application/json"); //json-rpc
          return request.send(jsonRpcRequest.toJson().toBuffer());
        })
        .compose(HttpClientResponse::body)
        .map(buffer -> {
          JsonRpcResponse jsonRpcResponse = JsonRpcResponse.fromJson(buffer.toJsonObject());
          if (jsonRpcResponse.isSuccess()) {
            return (JsonObject) jsonRpcResponse.getResult();
          } else {
            throw new RuntimeException("JSON-RPC error: " + jsonRpcResponse.getError());
          }
        });
    }
  }
}
