package io.vertx.mcp.server.impl;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.protobuf.Descriptors;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.transcoding.MethodTranscodingOptions;
import io.vertx.grpc.transcoding.impl.*;
import io.vertx.jrpc.mcp.proto.ModelContextProtocolProto;
import io.vertx.json.schema.*;
import io.vertx.mcp.*;
import io.vertx.mcp.bridge.grpc.ModelContextProtocolBridge;
import io.vertx.mcp.bridge.grpc.handler.*;
import io.vertx.mcp.jrpc.model.JsonRpcError;
import io.vertx.mcp.jrpc.model.JsonRpcRequest;
import io.vertx.mcp.proto.BlobResourceContent;
import io.vertx.mcp.proto.TextResourceContent;
import io.vertx.mcp.server.handler.*;

import java.net.URI;
import java.util.List;

public class ModelContextProtocolBridgeImpl implements ModelContextProtocolBridge {

  private static final ServiceName SERVICE_NAME = ServiceName.create("io.modelcontextprotocol.ModelContextProtocolServer");
  private static final Descriptors.ServiceDescriptor SERVICE_DESCRIPTOR = ModelContextProtocolProto.getDescriptor().findServiceByName("ModelContextProtocolServer");

  public static final Descriptors.Descriptor TEXT_RESOURCE_DESCRIPTOR = TextResourceContent.getDescriptor();
  public static final Descriptors.Descriptor BLOB_RESOURCE_DESCRIPTOR = BlobResourceContent.getDescriptor();

  private final Vertx vertx;
  private final ModelContextProtocolServer service;

  private GrpcServer grpcServer;

  public ModelContextProtocolBridgeImpl(Vertx vertx, ModelContextProtocolServer service) {
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

    grpcServer.services().forEach(service -> service.methodDescriptors().forEach(methodDescriptor -> {
      String fqn = service.name().fullyQualifiedName() + "/" + methodDescriptor.getName();

      if (methodDescriptor.getOptions().hasExtension(AnnotationsProto.http)) {
        HttpMethod method;
        String path;

        HttpRule httpRule = methodDescriptor.getOptions().getExtension(AnnotationsProto.http);
        switch (httpRule.getPatternCase()) {
          case GET:
            method = HttpMethod.GET;
            path = httpRule.getGet();
            break;
          case POST:
            method = HttpMethod.POST;
            path = httpRule.getPost();
            break;
          case PUT:
            method = HttpMethod.PUT;
            path = httpRule.getPut();
            break;
          case DELETE:
            method = HttpMethod.DELETE;
            path = httpRule.getDelete();
            break;
          case PATCH:
            method = HttpMethod.PATCH;
            path = httpRule.getPatch();
            break;
          default:
            method = null;
            path = null;
        }

        // Register a resource provider for each method (if marked as MCP resource in future we can filter)
        ModelContextProtocolResourceProvider resourceProvider = new BridgeClientResourceProvider(
          fqn,
          path,
          method,
          methodDescriptor
        );

        ModelContextProtocolResourceTemplate template = ModelContextProtocolResourceTemplate.create(
          methodDescriptor.getName(),
          methodDescriptor.getName(),
          methodDescriptor.getName(),
          "http://localhost" + path,
          "application/json"
        );

        this.service.registerResourceProvider(resourceProvider);
        this.service.registerResourceTemplate(template);
        //return; TODO: Better option processing?
      }

      ModelContextProtocolTool tool = createBridgeClientTool(
        fqn,
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
    server.callHandler(ResourcesTemplatesListHandler.SERVICE_METHOD, new ResourcesTemplatesListHandler(server, service));
    server.callHandler(ResourcesReadHandler.SERVICE_METHOD, new ResourcesReadHandler(server, service));
    server.callHandler(ResourcesSubscribeHandler.SERVICE_METHOD, new ResourcesSubscribeHandler(server, service));
    server.callHandler(ResourcesUnsubscribeHandler.SERVICE_METHOD, new ResourcesUnsubscribeHandler(server, service));
    server.callHandler(PromptsListHandler.SERVICE_METHOD, new PromptsListHandler(server, service));
    server.callHandler(PromptsGetHandler.SERVICE_METHOD, new PromptsGetHandler(server, service));
  }

  @Override
  public void handle(HttpServerRequest request) {
    if (grpcServer == null) {
      request.response().setStatusCode(500).end("Server not yet initialized");
      return;
    }

    if (request.method() == HttpMethod.GET && request.getHeader(HttpHeaders.ACCEPT).contains("text/event-stream")) {
      request.response().setStatusCode(405).end(JsonRpcError.methodNotAllowed().toJson().toBuffer());
      return;
    }

    // Check if this is a JSON-RPC request
    String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
    if (contentType == null || (!contentType.contains("application/json") && !contentType.contains("application/json-rpc"))) {
      grpcServer.handle(request);
      return;
    }

    Buffer body = Buffer.buffer();

    request.resume().handler(body::appendBuffer);
    request.endHandler(v -> {
      try {
        JsonObject bodyJson = body.toJsonObject();

        // Transform the method field from underscore format to PascalCase
        if (bodyJson.containsKey("method")) {
          String originalMethod = bodyJson.getString("method");
          String transformedMethod = toPascalCase(originalMethod);
          bodyJson.put("method", transformedMethod);
        }

        JsonRpcRequest jsonRpcRequest = JsonRpcRequest.fromJson(bodyJson);

        if (jsonRpcRequest.getMethod() == null) {
          sendJsonRpcError(request.response(), jsonRpcRequest.getId(), -32600, "Invalid Request: missing method");
          return;
        }

        String serviceName = request.path().substring(1);

        if (serviceName.isEmpty()) {
          sendJsonRpcError(request.response(), jsonRpcRequest.getId(), -32601, "Invalid Request: missing service name");
          return;
        }

        ProxyHttpServerRequestRequest transformedRequest = new ProxyHttpServerRequestRequest(
          (HttpServerRequestInternal) request, jsonRpcRequest.getMethod(),
          ServiceName.create(serviceName),
          jsonRpcRequest
        );
        grpcServer.handle(transformedRequest);
      } catch (Exception e) {
        sendJsonRpcError(request.response(), null, -32700, "Parse error: " + e.getMessage());
      }
    });
  }

  private String toPascalCase(String underscoreFormat) {
    if (underscoreFormat == null || underscoreFormat.isEmpty()) {
      return underscoreFormat;
    }

    String[] words = underscoreFormat.split("/");
    StringBuilder result = new StringBuilder();

    for (String word : words) {
      if (!word.isEmpty()) {
        result.append(Character.toUpperCase(word.charAt(0)));
        if (word.length() > 1) {
          result.append(word.substring(1).toLowerCase());
        }
      }
    }

    return result.toString();
  }

  private void sendJsonRpcError(HttpServerResponse response, Integer id, int code, String message) {
    JsonObject error = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("id", id)
      .put("error", new JsonObject()
        .put("code", code)
        .put("message", message));

    response.putHeader("Content-Type", "application/json-rpc").end(error.toBuffer());
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
    public ModelContextProtocolServer service() {
      return service;
    }

    @Override
    public Future<ContentDataType> apply(JsonObject parameters) {
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
      Promise<ContentDataType> resultPromise = Promise.promise();

      mockResponse.getResponseFuture().onComplete(ar -> {
        if (ar.succeeded()) {
          if (outputDescriptor.equals(SchemaUtil.CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ContentDataType.UnstructuredContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.TEXT_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ContentDataType.TextContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.IMAGE_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ContentDataType.ImageContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.AUDIO_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ContentDataType.AudioContentDataType.create(ar.result().toJsonObject()));
          } else if (outputDescriptor.equals(SchemaUtil.RESOURCE_LINK_CONTENT_DESCRIPTOR)) {
            resultPromise.complete(ContentDataType.ResourceLinkContentDataType.create(ar.result().toJsonObject()));
          }

          if (isStructured() && outputSchema != null) {
            OutputUnit outputResult = outputValidator.validate(ar.result().toJsonObject());
            if (!outputResult.getValid()) {
              resultPromise.fail(outputResult.getErrors().toString());
              return;
            }
          }

          resultPromise.complete(ContentDataType.StructuredJsonContentDataType.create(ar.result().toJsonObject()));
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

    private final String fqn;
    private final String path;
    private final HttpMethod method;
    private final PathMatcher pathMatcher;
    private final Descriptors.MethodDescriptor methodDescriptor;

    public BridgeClientResourceProvider(String fqn, String path, HttpMethod method, Descriptors.MethodDescriptor methodDescriptor) {
      this.fqn = fqn;
      this.path = path;
      this.method = method;
      this.methodDescriptor = methodDescriptor;

      PathMatcherBuilder builder = new PathMatcherBuilder();
      PathMatcherUtility.registerByHttpRule(builder, new MethodTranscodingOptions()
          .setSelector("selector")
          .setHttpMethod(method)
          .setPath(path)
          .setBody("*")
          .setResponseBody("*")
        , fqn
      );

      this.pathMatcher = builder.build();
    }

    @Override
    public Future<List<ModelContextProtocolResource>> apply(URI uri) {
      if (uri == null)
        return Future.succeededFuture(List.of());

      PathMatcherLookupResult res = pathMatcher.lookup(this.method.name(), uri.getPath(), "");
      if (res == null) {
        return Future.succeededFuture(null);
      }

      ModelContextProtocolServerRequest mockRequest = new ModelContextProtocolServerRequest(
        this.method,
        "/" + fqn,
        MessageWeaver.weaveRequestMessage(new JsonObject().toBuffer(), res.getVariableBindings(), null),
        vertx.getOrCreateContext()
      );

      ModelContextProtocolServerResponse mockResponse = (ModelContextProtocolServerResponse) mockRequest.response();

      //Future<String> textFuture = mockResponse.getResponseFuture().map(buffer -> buffer == null ? "" : buffer.toString());

      // Dispatch the request
      mockRequest.pause();
      grpcServer.handle(mockRequest);
      mockRequest.resume();

      if (methodDescriptor.getOutputType().equals(TEXT_RESOURCE_DESCRIPTOR)) {
        return mockResponse.getResponseFuture().map(buffer -> {
          if (buffer == null) {
            return List.of();
          }

          JsonObject text = buffer.toJsonObject();
          URI textUri = URI.create(text.getString("uri"));
          if (!textUri.isAbsolute()) {
            textUri = uri.resolve(textUri);
          }

          return List.of(ModelContextProtocolResource.TextResource.create(
            textUri,
            methodDescriptor.getName(),
            methodDescriptor.getName(),
            methodDescriptor.getName(),
            text.getString("mimeType"),
            Future.succeededFuture(text.getString("text"))));
        });
      }

      if (methodDescriptor.getOutputType().equals(BLOB_RESOURCE_DESCRIPTOR)) {
        return mockResponse.getResponseFuture().map(buffer -> {
          if (buffer == null) {
            return List.of();
          }

          JsonObject blob = buffer.toJsonObject();
          URI blobUri = URI.create(blob.getString("uri"));
          if (!blobUri.isAbsolute()) {
            blobUri = uri.resolve(blobUri);
          }

          return List.of(ModelContextProtocolResource.BinaryResource.create(
            blobUri,
            methodDescriptor.getName(),
            methodDescriptor.getName(),
            methodDescriptor.getName(),
            blob.getString("mimeType"),
            Future.succeededFuture(blob.getBuffer("blob"))
          ));
        });
      }

      return mockResponse.getResponseFuture().map(buffer -> List.of(ModelContextProtocolResource.TextResource.create(
        uri,
        methodDescriptor.getName(),
        methodDescriptor.getName(),
        methodDescriptor.getName(),
        "application/json",
        Future.succeededFuture(buffer == null ? "" : buffer.toString())
      )));
    }
  }
}
