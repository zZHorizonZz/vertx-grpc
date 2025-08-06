package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceProvider;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.ResourcesSubscribeRequest;
import io.vertx.jrpc.mcp.proto.ResourcesSubscribeResponse;

import java.util.Optional;

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
  public ResourcesSubscribeHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesSubscribeRequest, ResourcesSubscribeResponse> request) {
    request.handler(req -> {
      try {
        // Accept subscription for now without validating specific resource ids
        String subscriptionId = "sub_" + System.currentTimeMillis();
        ResourcesSubscribeResponse response = ResourcesSubscribeResponse.newBuilder()
          .setSuccess(true)
          .setSubscriptionId(subscriptionId)
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
