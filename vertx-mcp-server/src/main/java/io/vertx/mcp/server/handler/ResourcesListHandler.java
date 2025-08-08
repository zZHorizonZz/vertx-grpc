package io.vertx.mcp.server.handler;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.proto.Resource;
import io.vertx.jrpc.mcp.proto.ResourcesListRequest;
import io.vertx.jrpc.mcp.proto.ResourcesListResponse;
import io.vertx.mcp.ModelContextProtocolResource;
import io.vertx.mcp.ModelContextProtocolResourceProvider;
import io.vertx.mcp.ModelContextProtocolServer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for the ResourcesList RPC method.
 */
public class ResourcesListHandler extends BaseHandler<ResourcesListRequest, ResourcesListResponse> {

  public static final ServiceMethod<ResourcesListRequest, ResourcesListResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolServer"),
    "ResourcesList",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesListRequest.newBuilder()));

  /**
   * Creates a new resources list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesListHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesListRequest, ResourcesListResponse> request) {
    request.handler(req -> {
      try {
        String filter = req.getCursor();
        resourcesList(filter)
          .onSuccess(resources -> {
            ResourcesListResponse.Builder builder = ResourcesListResponse.newBuilder();
            resources.forEach(res -> builder.addResources(buildResource(res)));
            request.response().end(builder.build());
          })
          .onFailure(err -> request.response().status(GrpcStatus.INTERNAL).end());
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }

  private Future<List<ModelContextProtocolResource>> resourcesList(String filter) {
    List<ModelContextProtocolResourceProvider> providers = service.resourcesList();
    return Future
      .all(providers.stream().map(provider -> provider
          .apply(null)
          .map(res -> res.stream().filter(r -> r.name().contains(filter) || r.description().contains(filter) || r.title().contains(filter)).collect(Collectors.toList())))
        .collect(Collectors.toList()))
      .map(CompositeFuture::list);
  }

  private Resource buildResource(ModelContextProtocolResource res) {
    return Resource.newBuilder()
      .setUri(res.uri().toString())
      .setName(res.name())
      .setTitle(res.title())
      .setDescription(res.description())
      .setMimeType(res.mimeType())
      .build();
  }
}
