package io.vertx.jrpc.transcoding.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;

public class JrpcHttpHandler implements Handler<HttpServerRequest> {

  private GrpcServer grpcServer;

  public JrpcHttpHandler(GrpcServer grpcServer) {
    this.grpcServer = grpcServer;
  }

  @Override
  public void handle(HttpServerRequest request) {
    if (!(request instanceof HttpServerRequestInternal)) {
      // Not HTTP, pass through to gRPC server
      grpcServer.handle(request);
      return;
    }

    // Check if this is a JSON-RPC request
    String contentType = request.getHeader("Content-Type");
    if (contentType == null || (!contentType.contains("application/json") && !contentType.contains("application/json-rpc"))) {
      // Not JSON-RPC, pass through to gRPC server
      grpcServer.handle(request);
      return;
    }

    // Buffer the request body to read the JSON-RPC method
    Buffer body = Buffer.buffer();

    request.resume().handler(body::appendBuffer);

    request.endHandler(v -> {
      try {
        JsonRpcRequest jsonRpcRequest = JsonRpcRequest.fromJson(body.toJsonObject());

        if (jsonRpcRequest.getMethod() == null) {
          sendJsonRpcError(request.response(), jsonRpcRequest.getId(), -32600, "Invalid Request: missing method");
          return;
        }

        String serviceName = request.path().substring(1);
        if (serviceName.isEmpty()) {
          serviceName = jsonRpcRequest.getMethod().contains("/") ? jsonRpcRequest.getMethod().substring(0, jsonRpcRequest.getMethod().indexOf("/")) : request.path().substring(1);
        }

        if (serviceName.isEmpty()) {
          sendJsonRpcError(request.response(), jsonRpcRequest.getId(), -32601, "Invalid Request: missing service name");
          return;
        }

        HttpProxyServerRequest transformedRequest = new HttpProxyServerRequest((HttpServerRequestInternal) request, jsonRpcRequest.getMethod(), ServiceName.create(serviceName),
          jsonRpcRequest);
        grpcServer.handle(transformedRequest);
      } catch (Exception e) {
        sendJsonRpcError(request.response(), null, -32700, "Parse error: " + e.getMessage());
      }
    });
  }

  private void sendJsonRpcError(HttpServerResponse response, Integer id, int code, String message) {
    JsonObject error = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("id", id)
      .put("error", new JsonObject()
        .put("code", code)
        .put("message", message));

    response
      .putHeader("Content-Type", "application/json-rpc")
      .end(error.toBuffer());
  }
}
