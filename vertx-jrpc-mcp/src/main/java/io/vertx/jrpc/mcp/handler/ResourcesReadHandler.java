package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceProvider;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.ResourcesReadRequest;
import io.vertx.jrpc.mcp.proto.ResourcesReadResponse;

import java.util.Optional;

/**
 * Handler for the ResourcesRead RPC method.
 */
public class ResourcesReadHandler extends BaseHandler<ResourcesReadRequest, ResourcesReadResponse> {

  public static final ServiceMethod<ResourcesReadRequest, ResourcesReadResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ResourcesRead",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesReadRequest.newBuilder()));

  /**
   * Creates a new resources read handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesReadHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesReadRequest, ResourcesReadResponse> request) {
    request.handler(req -> {
      try {
        String resourceId = req.getResourceId();
        Optional<ModelContextProtocolResourceProvider> res = service.resourcesList().stream()
          .filter(r -> r.id().equals(resourceId))
          .findFirst();
        if (res.isEmpty()) {
          request.response().status(GrpcStatus.INTERNAL).end();
          return;
        }
        ResourcesReadResponse response = ResourcesReadResponse.newBuilder()
          .setContent("This is the content of resource " + resourceId)
          .setContentType("text/plain")
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
