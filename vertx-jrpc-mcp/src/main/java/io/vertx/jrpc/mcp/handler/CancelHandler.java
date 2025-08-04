package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.CancelRequest;
import io.vertx.jrpc.mcp.proto.CancelResponse;
import io.vertx.jrpc.mcp.proto.PingRequest;
import io.vertx.jrpc.mcp.proto.PingResponse;

/**
 * Handler for the Cancel RPC method.
 */
public class CancelHandler extends BaseHandler<CancelRequest, CancelResponse> {

  public static final ServiceMethod<CancelRequest, CancelResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "Cancel",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(CancelRequest.newBuilder()));

  /**
   * Creates a new cancel handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public CancelHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<CancelRequest, CancelResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.cancel(req)
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
