package io.vertx.mcp.bridge.grpc.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolService;
import io.vertx.mcp.ModelContextProtocolTool;
import io.vertx.jrpc.mcp.proto.Tool;
import io.vertx.jrpc.mcp.proto.ToolsListRequest;
import io.vertx.jrpc.mcp.proto.ToolsListResponse;

import java.util.stream.Collectors;

/**
 * Handler for the ToolsList RPC method.
 */
public class ToolsListHandler extends BaseHandler<ToolsListRequest, ToolsListResponse> {

  public static final ServiceMethod<ToolsListRequest, ToolsListResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ToolsList",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ToolsListRequest.newBuilder()));

  /**
   * Creates a new tools list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ToolsListHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ToolsListRequest, ToolsListResponse> request) {
    request.handler(req -> {
      try {
        toolsList(req)
          .onSuccess(response -> request.response().end(response))
          .onFailure(err -> request.response().status(GrpcStatus.INTERNAL).end());
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }

  /**
   * Lists available tools.
   *
   * @param request the tools list request
   * @return a future with the tools list response
   */
  public Future<ToolsListResponse> toolsList(ToolsListRequest request) {
    return Future.succeededFuture(ToolsListResponse.newBuilder()
      .addAllTools(service.toolsList().stream().map(ToolsListHandler::buildTool).collect(Collectors.toUnmodifiableSet()))
      .build());
  }

  private static Tool buildTool(ModelContextProtocolTool tool) {
    Struct.Builder inputSchemaStruct = Struct.newBuilder();
    Struct.Builder outputSchemaStruct = Struct.newBuilder();

    try {
      JsonObject inputSchema = tool.inputSchema() instanceof JsonObject ? (JsonObject) tool.inputSchema() : defaultSchema();
      JsonObject outputSchema = tool.outputSchema() instanceof JsonObject ? (JsonObject) tool.outputSchema() : defaultSchema();

      JsonFormat.parser().merge(inputSchema.encode(), inputSchemaStruct);
      JsonFormat.parser().merge(outputSchema.encode(), outputSchemaStruct);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    Tool.Builder builder = Tool.newBuilder()
      .setName(tool.name())
      .setTitle(tool.title())
      .setDescription(tool.description())
      .setInputSchema(inputSchemaStruct);

    if (tool.outputSchema() != null) {
      builder.setOutputSchema(outputSchemaStruct);
    }

    return builder.build();
  }

  private static JsonObject defaultSchema() {
    JsonObject inputSchema = new JsonObject();
    inputSchema.put("type", "object");

    inputSchema.put("properties", new JsonObject());
    inputSchema.put("additionalProperties", false);
    inputSchema.put("$schema", "http://json-schema.org/draft-07/schema#");

    return inputSchema;
  }
}
