package io.vertx.jrpc.transcoding.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.internal.http.HttpServerRequestWrapper;
import io.vertx.grpc.common.ServiceName;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;

public class HttpProxyServerRequest extends HttpServerRequestWrapper {

  private String methodName;
  private ServiceName serviceName;

  private final JsonRpcRequest jsonRpcRequest;

  public HttpProxyServerRequest(HttpServerRequestInternal delegate, String methodName, ServiceName serviceName, JsonRpcRequest jsonRpcRequest) {
    super(delegate);
    this.methodName = methodName;
    this.serviceName = serviceName;
    this.jsonRpcRequest = jsonRpcRequest;
  }

  @Override
  public String path() {
    return serviceName.pathOf(methodName);
  }

  @Override
  public HttpServerRequest handler(Handler<Buffer> handler) {
    return this;
  }

  @Override
  public HttpServerRequest endHandler(Handler<Void> handler) {
    return this;
  }

  public Buffer getTransformedBody() {
    return jsonRpcRequest.toBuffer();
  }

  public JsonRpcRequest getJsonRpcRequest() {
    return jsonRpcRequest;
  }
}
