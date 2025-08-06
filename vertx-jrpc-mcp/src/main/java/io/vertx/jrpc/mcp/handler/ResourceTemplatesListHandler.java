package io.vertx.jrpc.mcp.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceProvider;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceTemplate;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.ResourceTemplate;
import io.vertx.jrpc.mcp.proto.ResourcesTemplatesListRequest;
import io.vertx.jrpc.mcp.proto.ResourcesTemplatesListResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for the ResourcesTemplatesList RPC method.
 */
public class ResourceTemplatesListHandler extends BaseHandler<ResourcesTemplatesListRequest, ResourcesTemplatesListResponse> {

  public static final ServiceMethod<ResourcesTemplatesListRequest, ResourcesTemplatesListResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ResourcesTemplatesList",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesTemplatesListRequest.newBuilder()));

  /**
   * Creates a new resource templates list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourceTemplatesListHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesTemplatesListRequest, ResourcesTemplatesListResponse> request) {
    request.handler(req -> {
      try {
        String filter = req.getCursor();
        List<ModelContextProtocolResourceProvider> providers = service.resourcesList();
        if (filter != null && !filter.isEmpty()) {
          providers = providers.stream()
            .filter(p -> {
              ModelContextProtocolResourceTemplate t = p.template();
              return t.name().contains(filter) || t.description().contains(filter) || t.title().contains(filter);
            })
            .collect(Collectors.toList());
        }

        List<ResourceTemplate> templates = providers.stream().map(p -> {
          ModelContextProtocolResourceTemplate t = p.template();
          return ResourceTemplate.newBuilder()
            .setUriTemplate(t.uriTemplate())
            .setName(t.name())
            .setTitle(t.title())
            .setDescription(t.description())
            .setMimeType(t.mimeType())
            .build();
        }).collect(Collectors.toList());

        ResourcesTemplatesListResponse response = ResourcesTemplatesListResponse.newBuilder()
          .addAllResourceTemplates(templates)
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
