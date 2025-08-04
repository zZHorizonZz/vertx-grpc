package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;
import io.vertx.jrpc.mcp.proto.PingRequest;
import io.vertx.jrpc.mcp.proto.PingResponse;

/**
 * Handler for the Ping RPC method.
 */
public class PingHandler extends BaseHandler<PingRequest, PingResponse> {

  public static final ServiceMethod<PingRequest, PingResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "Ping",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(PingRequest.newBuilder()));

  /**
   * Creates a new ping handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public PingHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PingRequest, PingResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.ping(req)
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
