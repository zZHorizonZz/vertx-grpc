package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;
import io.vertx.jrpc.mcp.proto.ResourcesSubscribeRequest;
import io.vertx.jrpc.mcp.proto.ResourcesSubscribeResponse;

/**
 * Handler for the ResourcesSubscribe RPC method.
 */
public class ResourcesSubscribeHandler extends BaseHandler<ResourcesSubscribeRequest, ResourcesSubscribeResponse> {

  public static final ServiceMethod<ResourcesSubscribeRequest, ResourcesSubscribeResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ResourcesSubscribe",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesSubscribeRequest.newBuilder()));

  /**
   * Creates a new resources subscribe handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesSubscribeHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesSubscribeRequest, ResourcesSubscribeResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.resourcesSubscribe(req)
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
