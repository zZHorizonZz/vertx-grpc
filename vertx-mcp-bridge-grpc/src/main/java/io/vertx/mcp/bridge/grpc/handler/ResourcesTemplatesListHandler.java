package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.proto.ResourceTemplate;
import io.vertx.jrpc.mcp.proto.ResourcesTemplatesListRequest;
import io.vertx.jrpc.mcp.proto.ResourcesTemplatesListResponse;
import io.vertx.mcp.ModelContextProtocolResourceTemplate;
import io.vertx.mcp.ModelContextProtocolServer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for the ResourcesTemplatesList RPC method.
 */
public class ResourcesTemplatesListHandler extends BaseHandler<ResourcesTemplatesListRequest, ResourcesTemplatesListResponse> {

  public static final ServiceMethod<ResourcesTemplatesListRequest, ResourcesTemplatesListResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolServer"),
    "ResourcesTemplatesList",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesTemplatesListRequest.newBuilder()));

  /**
   * Creates a new resource templates list handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesTemplatesListHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesTemplatesListRequest, ResourcesTemplatesListResponse> request) {
    request.handler(req -> {
      try {
        String filter = req.getCursor();
        List<ModelContextProtocolResourceTemplate> templates = service.resourcesTemplatesList();
        if (!filter.isEmpty()) {
          templates = templates.stream()
            .filter(t -> t.name().contains(filter) || t.description().contains(filter) || t.title().contains(filter))
            .collect(Collectors.toList());
        }

        ResourcesTemplatesListResponse response = ResourcesTemplatesListResponse.newBuilder()
          .addAllResourceTemplates(templates.stream().map(t -> ResourceTemplate.newBuilder()
            .setUriTemplate(t.uriTemplate())
            .setName(t.name())
            .setTitle(t.title())
            .setDescription(t.description())
            .setMimeType(t.mimeType())
            .build()).collect(Collectors.toList()))
          .build();
        request.response().end(response);
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }
}
