package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.jrpc.mcp.proto.PromptsListRequest;
import io.vertx.jrpc.mcp.proto.PromptsListResponse;

/**
 * Handler for the PromptsList RPC method.
 */
public class PromptsListHandler extends BaseHandler<PromptsListRequest, PromptsListResponse> {

  public static final ServiceMethod<PromptsListRequest, PromptsListResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "PromptsList",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(PromptsListRequest.newBuilder()));

  /**
   * Creates a new prompts list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public PromptsListHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PromptsListRequest, PromptsListResponse> request) {
    request.handler(req -> {
      try {
        PromptsListResponse response = PromptsListResponse.newBuilder()
          //.addAllPrompts(service.promptProviders().stream().map(ModelContextProtocolPromptProvider::prompt).collect(Collectors.toUnmodifiableSet()))
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
