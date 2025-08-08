package io.vertx.mcp.bridge.grpc;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.grpc.server.Service;
import io.vertx.mcp.ModelContextProtocolService;
import io.vertx.mcp.bridge.grpc.impl.ModelContextProtocolBridgeImpl;

public interface ModelContextProtocolBridge extends Service, Handler<HttpServerRequest> {
  static ModelContextProtocolBridge create(Vertx vertx, ModelContextProtocolService service) {
    return new ModelContextProtocolBridgeImpl(vertx, service);
  }
}
