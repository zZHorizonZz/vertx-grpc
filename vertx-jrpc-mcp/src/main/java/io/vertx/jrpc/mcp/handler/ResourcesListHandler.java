package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;
import io.vertx.jrpc.mcp.proto.ResourcesListRequest;
import io.vertx.jrpc.mcp.proto.ResourcesListResponse;

/**
 * Handler for the ResourcesList RPC method.
 */
public class ResourcesListHandler extends BaseHandler<ResourcesListRequest, ResourcesListResponse> {

  public static final ServiceMethod<ResourcesListRequest, ResourcesListResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ResourcesList",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesListRequest.newBuilder()));

  /**
   * Creates a new resources list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesListHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesListRequest, ResourcesListResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.resourcesList(req)
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
