package io.vertx.jrpc.mcp.impl;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.jrpc.transcoding.model.JsonRpcError;

public class ModelContextProtocolProxyHandler implements Handler<HttpServerRequest> {

  private final Handler<HttpServerRequest> server;

  public ModelContextProtocolProxyHandler(Handler<HttpServerRequest> server) {
    this.server = server;
  }

  @Override
  public void handle(HttpServerRequest request) {
    System.out.println("Handling request: " + request.path() + " " + request.method() + " " + request.headers());
    if (request.method() == HttpMethod.GET && request.getHeader(HttpHeaders.ACCEPT).contains("text/event-stream")) {
      request.response().setStatusCode(405).end(JsonRpcError.methodNotAllowed().toJson().toBuffer());
    } else {
      /*request.response().headersEndHandler(v -> {
        request.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream");
      });*/

      server.handle(request);
    }
  }
}
