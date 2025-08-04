package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;
import io.vertx.jrpc.mcp.proto.ResourcesUnsubscribeRequest;
import io.vertx.jrpc.mcp.proto.ResourcesUnsubscribeResponse;

/**
 * Handler for the ResourcesUnsubscribe RPC method.
 */
public class ResourcesUnsubscribeHandler extends BaseHandler<ResourcesUnsubscribeRequest, ResourcesUnsubscribeResponse> {

  public static final ServiceMethod<ResourcesUnsubscribeRequest, ResourcesUnsubscribeResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ResourcesUnsubscribe",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesUnsubscribeRequest.newBuilder()));

  /**
   * Creates a new resources unsubscribe handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesUnsubscribeHandler(GrpcServer server, ModelContextProtocolServiceImpl service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesUnsubscribeRequest, ResourcesUnsubscribeResponse> request) {
    request.handler(req -> {
      try {
        // Call the service implementation method
        service.resourcesUnsubscribe(req)
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
