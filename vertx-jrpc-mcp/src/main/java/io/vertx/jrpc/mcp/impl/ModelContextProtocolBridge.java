package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.ModelContextProtocolTool;
import io.vertx.jrpc.mcp.proto.Tool;

public class ModelContextProtocolBridge {

  private final Vertx vertx;
  private final ModelContextProtocolService mcpService;

  private GrpcServer grpcServer;

  public ModelContextProtocolBridge(Vertx vertx, ModelContextProtocolService mcpService) {
    this.vertx = vertx;
    this.mcpService = mcpService;
  }

  public ModelContextProtocolBridge bind(GrpcServer grpcServer) {
    this.grpcServer = grpcServer;

    mcpService.bind(grpcServer);

    grpcServer.services().forEach(service -> service.methodDescriptors().forEach(methodDescriptor -> {
      ModelContextProtocolTool tool = createBridgeClientTool(
        service.name().fullyQualifiedName() + "/" + methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName() + " trough mcp",
        methodDescriptor
      );
      mcpService.registerTool(tool);
    }));

    return this;
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

    private final JsonObject inputSchema;
    private final JsonObject outputSchema;

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
    }

    @Override
    public String id() {
      return name;
    }

    @Override
    public Tool tool() {
      Struct.Builder inputSchemaStruct = Struct.newBuilder();
      Struct.Builder outputSchemaStruct = Struct.newBuilder();

      try {
        JsonObject inputSchema = this.inputSchema != null ? this.inputSchema : defaultSchema();
        JsonObject outputSchema = this.outputSchema != null ? this.outputSchema : defaultSchema();

        JsonFormat.parser().merge(inputSchema.encode(), inputSchemaStruct);
        JsonFormat.parser().merge(outputSchema.encode(), outputSchemaStruct);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }

      Tool.Builder tool = Tool.newBuilder()
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setInputSchema(inputSchemaStruct);

      if (isStructured()) {
        tool.setOutputSchema(outputSchemaStruct);
      }

      return tool.build();
    }

    private boolean isStructured() {
      return outputDescriptor != SchemaUtil.CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.TEXT_CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.IMAGE_CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.AUDIO_CONTENT_DESCRIPTOR && outputDescriptor != SchemaUtil.RESOURCE_LINK_CONTENT_DESCRIPTOR;
    }

    private JsonObject defaultSchema() {
      JsonObject inputSchema = new JsonObject();
      inputSchema.put("type", "object");

      inputSchema.put("properties", new JsonObject());
      inputSchema.put("additionalProperties", false);
      inputSchema.put("$schema", "http://json-schema.org/draft-07/schema#");

      return inputSchema;
    }

    @Override
    public ModelContextProtocolService service() {
      return mcpService;
    }

    @Override
    public Future<ModelContextProtocolTool.ContentDataType> apply(JsonObject parameters) {
      ModelContextProtocolServerRequest mockRequest = new ModelContextProtocolServerRequest(
        HttpMethod.POST,
        "/" + fqn,
        parameters.toBuffer(),
        vertx.getOrCreateContext()
      );

      ModelContextProtocolServerResponse mockResponse = (ModelContextProtocolServerResponse) mockRequest.response();
      Promise<ModelContextProtocolTool.ContentDataType> resultPromise = Promise.promise();

      mockResponse.getResponseFuture().onComplete(ar -> {
        if (ar.succeeded()) {
          if (outputDescriptor.equals(SchemaUtil.CONTENT_DESCRIPTOR)) {
            resultPromise.complete(UnstructuredContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.TEXT_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(TextContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.IMAGE_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ImageContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.AUDIO_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(AudioContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.RESOURCE_LINK_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ResourceLinkContentDataType.create(ar.result().toJsonObject()));
          }

          resultPromise.complete(StructuredJsonContentDataType.create(ar.result().toJsonObject()));
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
}
