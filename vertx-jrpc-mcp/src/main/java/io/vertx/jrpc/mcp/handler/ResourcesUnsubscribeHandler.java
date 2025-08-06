package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
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
  public ResourcesUnsubscribeHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesUnsubscribeRequest, ResourcesUnsubscribeResponse> request) {
    request.handler(req -> {
      try {
        ResourcesUnsubscribeResponse response = ResourcesUnsubscribeResponse.newBuilder()
          .setSuccess(true)
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
