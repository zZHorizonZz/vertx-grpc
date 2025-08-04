package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.ToolsCallRequest;
import io.vertx.jrpc.mcp.proto.ToolsCallResponse;

/**
 * Handler for the ToolsCall RPC method.
 */
public class ToolsCallHandler extends BaseHandler<ToolsCallRequest, ToolsCallResponse> {

  /**
   * Creates a new tools call handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ToolsCallHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ToolsCallRequest, ToolsCallResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.toolsCall(req)
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
