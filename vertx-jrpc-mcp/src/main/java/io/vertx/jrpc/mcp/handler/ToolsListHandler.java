package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.ToolsListRequest;
import io.vertx.jrpc.mcp.proto.ToolsListResponse;

/**
 * Handler for the ToolsList RPC method.
 */
public class ToolsListHandler extends BaseHandler<ToolsListRequest, ToolsListResponse> {

  /**
   * Creates a new tools list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ToolsListHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ToolsListRequest, ToolsListResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.toolsList(req)
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
