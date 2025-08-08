package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.jrpc.mcp.proto.PromptsGetRequest;
import io.vertx.jrpc.mcp.proto.PromptsGetResponse;

/**
 * Handler for the PromptsGet RPC method.
 */
public class PromptsGetHandler extends BaseHandler<PromptsGetRequest, PromptsGetResponse> {

  public static final ServiceMethod<PromptsGetRequest, PromptsGetResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "PromptsGet",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(PromptsGetRequest.newBuilder()));

  /**
   * Creates a new prompts get handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public PromptsGetHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PromptsGetRequest, PromptsGetResponse> request) {
    request.handler(req -> {
      try {
        String promptId = req.getPromptId();
        boolean exists = service.promptProviders().stream().anyMatch(p -> p.id().equals(promptId));
        if (!exists) {
          request.response().status(GrpcStatus.INTERNAL).end();
          return;
        }
        PromptsGetResponse response = PromptsGetResponse.newBuilder()
          .setContent("This is the content of prompt " + promptId)
          .putMetadata("author", "Vert.x")
          .putMetadata("version", "1.0")
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
