package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.PromptsGetRequest;
import io.vertx.jrpc.mcp.proto.PromptsGetResponse;

/**
 * Handler for the PromptsGet RPC method.
 */
public class PromptsGetHandler extends BaseHandler<PromptsGetRequest, PromptsGetResponse> {

  /**
   * Creates a new prompts get handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public PromptsGetHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PromptsGetRequest, PromptsGetResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.promptsGet(req)
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
