package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;
import io.vertx.jrpc.mcp.proto.ResourcesReadRequest;
import io.vertx.jrpc.mcp.proto.ResourcesReadResponse;

/**
 * Handler for the ResourcesRead RPC method.
 */
public class ResourcesReadHandler extends BaseHandler<ResourcesReadRequest, ResourcesReadResponse> {

  public static final ServiceMethod<ResourcesReadRequest, ResourcesReadResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "Resources/Read",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesReadRequest.newBuilder()));

  /**
   * Creates a new resources read handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesReadHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesReadRequest, ResourcesReadResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.resourcesRead(req)
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
