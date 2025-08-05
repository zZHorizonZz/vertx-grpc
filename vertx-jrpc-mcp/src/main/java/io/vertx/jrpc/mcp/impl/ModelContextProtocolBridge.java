package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.ModelContextProtocolTool;
import io.vertx.jrpc.mcp.proto.Tool;

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
        service.name().fullyQualifiedName() + "/" + methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName() + " trough mcp",
        JsonInputSchemaUtil.toJsonSchema(methodDescriptor.getInputType()),
        JsonInputSchemaUtil.toJsonSchema(methodDescriptor.getOutputType())
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
  public ModelContextProtocolTool createHttpClientTool(String fqn, String name, String methodName, String description, JsonObject inputSchema, JsonObject outputSchema) {
    return new HttpClientTool(fqn, name, methodName, description, inputSchema, outputSchema);
  }

  /**
   * Tool implementation that uses HTTP client to call MCP methods via JSON-RPC.
   */
  private class HttpClientTool implements ModelContextProtocolTool {
    private final String fqn;
    private final String name;
    private final String title;
    private final String description;

    private final JsonObject inputSchema;
    private final JsonObject outputSchema;

    public HttpClientTool(String fqn, String name, String title, String description, JsonObject inputSchema, JsonObject outputSchema) {
      this.fqn = fqn;
      this.name = name;
      this.title = title;
      this.description = description;
      this.inputSchema = inputSchema;
      this.outputSchema = outputSchema;
    }

    @Override
    public String id() {
      return name;
    }

    @Override
    public Tool tool() {
      Struct.Builder inputSchemaStruct = Struct.newBuilder();
      Struct.Builder outputSchemaStruct = Struct.newBuilder();

      try {
        JsonObject inputSchema = this.inputSchema != null ? this.inputSchema : defaultSchema();
        JsonObject outputSchema = this.outputSchema != null ? this.outputSchema : defaultSchema();

        JsonFormat.parser().merge(inputSchema.encode(), inputSchemaStruct);
        JsonFormat.parser().merge(outputSchema.encode(), outputSchemaStruct);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }

      return Tool.newBuilder()
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setInputSchema(inputSchemaStruct)
        .setOutputSchema(outputSchemaStruct)
        .build();
    }

    private JsonObject defaultSchema() {
      JsonObject inputSchema = new JsonObject();
      inputSchema.put("type", "object");

      inputSchema.put("properties", new JsonObject());
      inputSchema.put("additionalProperties", false);
      inputSchema.put("$schema", "http://json-schema.org/draft-07/schema#");

      return inputSchema;
    }

    @Override
    public ModelContextProtocolService service() {
      return mcpService;
    }

    @Override
    public Future<JsonObject> apply(JsonObject parameters) {
      ModelContextProtocolServerRequest mockRequest = new ModelContextProtocolServerRequest(
        HttpMethod.POST,
        "/" + fqn,
        parameters.toBuffer(),
        vertx.getOrCreateContext()
      );

      // Get the mock response that's linked to the request
      ModelContextProtocolServerResponse mockResponse = (ModelContextProtocolServerResponse) mockRequest.response();

      // Set up a promise to track when the response is complete
      Promise<JsonObject> resultPromise = Promise.promise();

      // Set up the response completion handler
      mockResponse.getResponseFuture().onComplete(ar -> {
        if (ar.succeeded()) {
          resultPromise.complete(ar.result().toJsonObject());
        } else {
          resultPromise.fail(ar.cause());
        }
      });

      // Now pass the request to the gRPC server
      // The server will read from the request and write to the response asynchronously
      mockRequest.pause();
      grpcServer.handle(mockRequest);
      mockRequest.resume();

      // Return the future that will complete when the response is ready
      return resultPromise.future();
    }
  }
}
