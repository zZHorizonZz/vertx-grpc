package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolResourceProvider;
import io.vertx.mcp.ModelContextProtocolService;
import io.vertx.mcp.ModelContextProtocolResourceTemplate;
import io.vertx.jrpc.mcp.proto.Resource;
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
        List<ModelContextProtocolResourceProvider> providers = service.resourcesList();
        if (!filter.isEmpty()) {
          providers = providers.stream()
            .filter(p -> {
              ModelContextProtocolResourceTemplate t = p.template();
              return t.name().contains(filter) || t.description().contains(filter) || t.title().contains(filter);
            })
            .collect(Collectors.toList());
        }
        List<Resource> resources = providers.stream().map(p -> {
          ModelContextProtocolResourceTemplate t = p.template();
          return Resource.newBuilder()
            .setUri(t.uriTemplate())
            .setName(t.name())
            .setTitle(t.title())
            .setDescription(t.description())
            .setMimeType(t.mimeType())
            .build();
        }).collect(Collectors.toList());

        ResourcesListResponse response = ResourcesListResponse.newBuilder()
          .addAllResources(resources)
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
