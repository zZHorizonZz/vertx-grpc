package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.PromptsListRequest;
import io.vertx.jrpc.mcp.proto.PromptsListResponse;

/**
 * Handler for the PromptsList RPC method.
 */
public class PromptsListHandler extends BaseHandler<PromptsListRequest, PromptsListResponse> {

  /**
   * Creates a new prompts list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public PromptsListHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PromptsListRequest, PromptsListResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.promptsList(req)
          .onSuccess(response -> {
            // Send the response
            request.response().end(response);
          })
          .onFailure(err -> {
            // Handle errors
            request.response().status(GrpcStatus.INTERNAL).end();
          });
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
