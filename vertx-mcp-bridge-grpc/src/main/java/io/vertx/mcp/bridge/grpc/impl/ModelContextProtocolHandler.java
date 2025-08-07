package io.vertx.mcp.bridge.grpc.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.mcp.jrpc.model.JsonRpcError;
import io.vertx.mcp.jrpc.model.JsonRpcRequest;

public class ModelContextProtocolHandler implements Handler<HttpServerRequest> {

  private final GrpcServer grpcServer;

  public ModelContextProtocolHandler(GrpcServer grpcServer) {
    this.grpcServer = grpcServer;
  }

  @Override
  public void handle(HttpServerRequest request) {
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

        ModelContextProtocolProxyRequest transformedRequest = new ModelContextProtocolProxyRequest((HttpServerRequestInternal) request, jsonRpcRequest.getMethod(),
          ServiceName.create(serviceName),
          jsonRpcRequest);
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
}
