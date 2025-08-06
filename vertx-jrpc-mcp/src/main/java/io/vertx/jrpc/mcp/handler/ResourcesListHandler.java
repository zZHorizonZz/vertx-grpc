package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceProvider;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.ResourcesListRequest;
import io.vertx.jrpc.mcp.proto.ResourcesListResponse;

import java.util.List;
import java.util.stream.Collectors;

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
  public ResourcesListHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesListRequest, ResourcesListResponse> request) {
    request.handler(req -> {
      try {
        String filter = req.getCursor();
        List<ModelContextProtocolResourceProvider> resources = service.resourcesList();
        if (!filter.isEmpty()) {
          resources = resources.stream()
            .filter(resource -> resource.resource().getName().contains(filter) ||
              resource.resource().getDescription().contains(filter))
            .collect(Collectors.toList());
        }
        ResourcesListResponse response = ResourcesListResponse.newBuilder()
          .addAllResources(resources.stream().map(ModelContextProtocolResourceProvider::resource).collect(Collectors.toUnmodifiableSet()))
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
