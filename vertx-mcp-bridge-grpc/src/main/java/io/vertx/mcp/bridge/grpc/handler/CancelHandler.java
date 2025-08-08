package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolServer;
import io.vertx.jrpc.mcp.proto.CancelRequest;
import io.vertx.jrpc.mcp.proto.CancelResponse;

/**
 * Handler for the Cancel RPC method.
 */
public class CancelHandler extends BaseHandler<CancelRequest, CancelResponse> {

  public static final ServiceMethod<CancelRequest, CancelResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolServer"),
    "Cancel",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(CancelRequest.newBuilder()));

  /**
   * Creates a new cancel handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public CancelHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<CancelRequest, CancelResponse> request) {
    request.handler(req -> {
      try {
        int requestId = Integer.parseInt(req.getRequestId());
        boolean success = service.cancelRequest(requestId);
        CancelResponse response = CancelResponse.newBuilder().setSuccess(success).build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
