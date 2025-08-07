package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolPromptProvider;
import io.vertx.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.PromptsListRequest;
import io.vertx.jrpc.mcp.proto.PromptsListResponse;

import java.util.stream.Collectors;

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
  public PromptsListHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PromptsListRequest, PromptsListResponse> request) {
    request.handler(req -> {
      try {
        PromptsListResponse response = PromptsListResponse.newBuilder()
          //.addAllPrompts(service.promptsList().stream().map(ModelContextProtocolPromptProvider::prompt).collect(Collectors.toUnmodifiableSet()))
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
