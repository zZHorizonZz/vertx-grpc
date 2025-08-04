package io.vertx.jrpc.mcp.handler;

import io.vertx.core.Handler;
import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;

/**
 * Handler for the Initialize RPC method.
 */
public class InitializeHandler implements Handler<GrpcServerRequest<InitializeRequest, InitializeResponse>> {

  public static final ServiceMethod<InitializeRequest, InitializeResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "Initialize",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(InitializeRequest.newBuilder()));

  private final GrpcServer server;
  private final ModelContextProtocolServiceImpl service;

  /**
   * Creates a new initialize handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public InitializeHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    this.server = server;
    this.service = service;
  }

  @Override
  public void handle(GrpcServerRequest<InitializeRequest, InitializeResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.initialize(req)
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
