package io.vertx.mcp.bridge.grpc.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Structs;
import com.google.protobuf.util.Values;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ContentDataType;
import io.vertx.mcp.ModelContextProtocolServer;
import io.vertx.jrpc.mcp.proto.ToolsCallRequest;
import io.vertx.jrpc.mcp.proto.ToolsCallResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for the ToolsCall RPC method.
 */
public class ToolsCallHandler extends BaseHandler<ToolsCallRequest, ToolsCallResponse> {

  public static final ServiceMethod<ToolsCallRequest, ToolsCallResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolServer"),
    "ToolsCall",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ToolsCallRequest.newBuilder()));

  private final JsonFormat.Parser jsonParser = JsonFormat.parser();
  private final JsonFormat.Printer jsonPrinter = JsonFormat.printer();

  public ToolsCallHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ToolsCallRequest, ToolsCallResponse> request) {
    request.handler(req -> processToolCall(req)
      .onSuccess(response -> request.response().end(response))
      .onFailure(err -> handleError(request, err))
    );
  }

  /**
   * Processes a tool call request and returns the response.
   */
  private Future<ToolsCallResponse> processToolCall(ToolsCallRequest request) {
    try {
      String toolName = request.getName();
      JsonObject parameters = extractParameters(request);
      return service.executeTool(toolName, parameters).map(this::buildResponse);
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  /**
   * Extracts parameters from the request.
   */
  private JsonObject extractParameters(ToolsCallRequest request) {
    try {
      String arguments = jsonPrinter.print(request.getArguments());
      return new JsonObject(arguments);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Failed to parse request arguments", e);
    }
  }

  /**
   * Builds the response based on the content data type.
   */
  private ToolsCallResponse buildResponse(ContentDataType result) {
    ToolsCallResponse.Builder responseBuilder = ToolsCallResponse.newBuilder();

    if (result instanceof ContentDataType.StructuredJsonContentDataType) {
      return buildStructuredResponse(responseBuilder, result);
    }

    if (result instanceof ContentDataType.UnstructuredContentDataType) {
      return buildUnstructuredResponse(responseBuilder, result);
    }

    return buildSimpleResponse(responseBuilder, result);
  }

  /**
   * Builds a structured JSON response.
   */
  private ToolsCallResponse buildStructuredResponse(ToolsCallResponse.Builder builder, ContentDataType result) {
    Struct structuredContent = parseToStruct(result.toJson().encode());
    List<Struct> content = createTextContent(result.toJson().encode());
    return builder.addAllContent(content).setStructuredContent(structuredContent).build();
  }

  /**
   * Builds an unstructured response with multiple content items.
   */
  private ToolsCallResponse buildUnstructuredResponse(ToolsCallResponse.Builder builder, ContentDataType result) {
    ContentDataType.UnstructuredContentDataType unstructured = (ContentDataType.UnstructuredContentDataType) result;
    List<Struct> content = unstructured.content().stream()
      .map(item -> parseToStruct(item.toJson().encode()))
      .collect(Collectors.toList());

    return builder.addAllContent(content).build();
  }

  /**
   * Builds a simple response with single content.
   */
  private ToolsCallResponse buildSimpleResponse(ToolsCallResponse.Builder builder, ContentDataType result) {
    Struct content = parseToStruct(result.toJson().encode());
    return builder.addContent(content).build();
  }

  /**
   * Creates a text content structure.
   */
  private List<Struct> createTextContent(String text) {
    Struct textStruct = Structs.of(
      "type", Values.of("text"),
      "text", Values.of(text)
    );
    return Collections.singletonList(textStruct);
  }

  /**
   * Parses JSON string to Protobuf Struct.
   */
  private Struct parseToStruct(String json) {
    try {
      Struct.Builder builder = Struct.newBuilder();
      jsonParser.merge(json, builder);
      return builder.build();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Failed to parse JSON to Struct", e);
    }
  }

  /**
   * Handles errors by sending appropriate gRPC status.
   */
  private void handleError(GrpcServerRequest<ToolsCallRequest, ToolsCallResponse> request, Throwable error) {
    GrpcStatus status = determineErrorStatus(error);
    request.response().status(status).end();
  }

  /**
   * Determines the appropriate gRPC status based on the error type.
   */
  private GrpcStatus determineErrorStatus(Throwable error) {
    if (error instanceof IllegalArgumentException) {
      return GrpcStatus.INVALID_ARGUMENT;
    }
    return GrpcStatus.INTERNAL;
  }
}
