package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.Descriptors;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.*;
import io.vertx.jrpc.mcp.handler.*;
import io.vertx.jrpc.mcp.proto.ModelContextProtocolProto;
import io.vertx.json.schema.*;
import io.vertx.mcp.proto.ModelContextProtocolAnnotations;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class ModelContextProtocolBridge implements Service {

  private static final ServiceName SERVICE_NAME = ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService");
  private static final Descriptors.ServiceDescriptor SERVICE_DESCRIPTOR = ModelContextProtocolProto.getDescriptor().findServiceByName("ModelContextProtocolService");

  private final Vertx vertx;
  private final ModelContextProtocolService service;

  private GrpcServer grpcServer;

  public ModelContextProtocolBridge(Vertx vertx, ModelContextProtocolService service) {
    this.vertx = vertx;
    this.service = service;
  }

  @Override
  public ServiceName name() {
    return SERVICE_NAME;
  }

  @Override
  public Descriptors.ServiceDescriptor descriptor() {
    return SERVICE_DESCRIPTOR;
  }

  @Override
  public void bind(GrpcServer server) {
    this.grpcServer = server;

    ModelContextProtocolResourceProvider resource = new BridgeClientResourceProvider();
    this.service.registerResourceProvider(resource);

    grpcServer.services().forEach(service -> service.methodDescriptors().forEach(methodDescriptor -> {
      if (methodDescriptor.getOptions().hasExtension(ModelContextProtocolAnnotations.mcpResource)) {
        return;
      }

      ModelContextProtocolTool tool = createBridgeClientTool(
        service.name().fullyQualifiedName() + "/" + methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName() + " trough mcp",
        methodDescriptor
      );
      this.service.registerTool(tool);
    }));

    server.callHandler(InitializeHandler.SERVICE_METHOD, new InitializeHandler(server, service));
    server.callHandler(PingHandler.SERVICE_METHOD, new PingHandler(server, service));
    server.callHandler(CancelHandler.SERVICE_METHOD, new CancelHandler(server, service));
    server.callHandler(ToolsListHandler.SERVICE_METHOD, new ToolsListHandler(server, service));
    server.callHandler(ToolsCallHandler.SERVICE_METHOD, new ToolsCallHandler(server, service));
    server.callHandler(ResourcesListHandler.SERVICE_METHOD, new ResourcesListHandler(server, service));
    server.callHandler(ResourcesReadHandler.SERVICE_METHOD, new ResourcesReadHandler(server, service));
    server.callHandler(ResourcesSubscribeHandler.SERVICE_METHOD, new ResourcesSubscribeHandler(server, service));
    server.callHandler(ResourcesUnsubscribeHandler.SERVICE_METHOD, new ResourcesUnsubscribeHandler(server, service));
    server.callHandler(PromptsListHandler.SERVICE_METHOD, new PromptsListHandler(server, service));
    server.callHandler(PromptsGetHandler.SERVICE_METHOD, new PromptsGetHandler(server, service));
  }

  public ModelContextProtocolTool createBridgeClientTool(String fqn, String name, String methodName, String description, Descriptors.MethodDescriptor methodDescriptor) {
    return new BridgeClientTool(fqn, name, methodName, description, methodDescriptor);
  }

  private class BridgeClientTool implements ModelContextProtocolTool {
    private final String fqn;
    private final String name;
    private final String title;
    private final String description;

    private final Descriptors.MethodDescriptor methodDescriptor;
    private final Descriptors.Descriptor inputDescriptor;
    private final Descriptors.Descriptor outputDescriptor;

    private final JsonSchema inputSchema;
    private final JsonSchema outputSchema;

    private final Validator inputValidator;
    private final Validator outputValidator;

    public BridgeClientTool(String fqn, String name, String title, String description, Descriptors.MethodDescriptor methodDescriptor) {
      this.fqn = fqn;
      this.name = name;
      this.title = title;
      this.description = description;
      this.methodDescriptor = methodDescriptor;
      this.inputSchema = SchemaUtil.toJsonSchema(methodDescriptor.getInputType());
      this.outputSchema = SchemaUtil.toJsonSchema(methodDescriptor.getOutputType());

      this.inputDescriptor = methodDescriptor.getInputType();
      this.outputDescriptor = methodDescriptor.getOutputType();

      this.inputValidator = Validator.create(inputSchema, new JsonSchemaOptions().setBaseUri("http://localhost/").setDraft(Draft.DRAFT7));
      this.outputValidator = Validator.create(outputSchema, new JsonSchemaOptions().setBaseUri("http://localhost/").setDraft(Draft.DRAFT7));
    }

    @Override
    public String id() {
      return name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String title() {
      return title;
    }

    @Override
    public String description() {
      return description;
    }

    @Override
    public JsonSchema inputSchema() {
      return inputSchema;
    }

    @Override
    public JsonSchema outputSchema() {
      return isStructured() ? outputSchema : null;
    }

    private boolean isStructured() {
      return outputDescriptor != SchemaUtil.CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.TEXT_CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.IMAGE_CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.AUDIO_CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.RESOURCE_LINK_CONTENT_DESCRIPTOR;
    }

    @Override
    public ModelContextProtocolService service() {
      return service;
    }

    @Override
    public Future<ModelContextProtocolDataType> apply(JsonObject parameters) {
      OutputUnit result = inputValidator.validate(parameters);
      if (!result.getValid()) {
        return Future.failedFuture(result.getErrors().toString());
      }

      ModelContextProtocolServerRequest mockRequest = new ModelContextProtocolServerRequest(
        HttpMethod.POST,
        "/" + fqn,
        parameters.toBuffer(),
        vertx.getOrCreateContext()
      );

      ModelContextProtocolServerResponse mockResponse = (ModelContextProtocolServerResponse) mockRequest.response();
      Promise<ModelContextProtocolDataType> resultPromise = Promise.promise();

      mockResponse.getResponseFuture().onComplete(ar -> {
        if (ar.succeeded()) {
          if (outputDescriptor.equals(SchemaUtil.CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ModelContextProtocolDataType.UnstructuredContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.TEXT_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ModelContextProtocolDataType.TextContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.IMAGE_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ModelContextProtocolDataType.ImageContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.AUDIO_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ModelContextProtocolDataType.AudioContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.RESOURCE_LINK_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ModelContextProtocolDataType.ResourceLinkContentDataType.create(ar.result().toJsonObject()));
          }

          if (isStructured() && outputSchema != null) {
            OutputUnit outputResult = outputValidator.validate(ar.result().toJsonObject());
            if (!outputResult.getValid()) {
              resultPromise.fail(outputResult.getErrors().toString());
              return;
            }
          }

          resultPromise.complete(ModelContextProtocolDataType.StructuredJsonContentDataType.create(ar.result().toJsonObject()));
        } else {
          resultPromise.fail(ar.cause());
        }
      });

      mockRequest.pause();
      grpcServer.handle(mockRequest);
      mockRequest.resume();

      return resultPromise.future();
    }
  }

  private class BridgeClientResourceProvider implements ModelContextProtocolResourceProvider {

    private final List<Service> services;
    private final List<Descriptors.MethodDescriptor> methodDescriptors;

    public BridgeClientResourceProvider() {
      this.services = grpcServer.services();
      this.methodDescriptors = services.stream()
        .flatMap(service -> service.methodDescriptors().stream())
        .filter(methodDescriptor -> methodDescriptor.getOptions().hasExtension(ModelContextProtocolAnnotations.mcpResource))
        .collect(Collectors.toList());
    }

    @Override
    public Future<ModelContextProtocolResource> apply(URI uri) {
      return null;
    }
  }
}
