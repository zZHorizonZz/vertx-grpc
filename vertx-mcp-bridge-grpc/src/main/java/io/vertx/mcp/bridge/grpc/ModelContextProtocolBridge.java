package io.vertx.mcp.bridge.grpc;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.grpc.server.Service;
import io.vertx.mcp.bridge.grpc.impl.ModelContextProtocolBridgeImpl;
import io.vertx.mcp.server.ModelContextProtocolServer;

public interface ModelContextProtocolBridge extends Service, Handler<HttpServerRequest> {
  static ModelContextProtocolBridge create(Vertx vertx, ModelContextProtocolServer service) {
    return new ModelContextProtocolBridgeImpl(vertx, service);
  }
}
