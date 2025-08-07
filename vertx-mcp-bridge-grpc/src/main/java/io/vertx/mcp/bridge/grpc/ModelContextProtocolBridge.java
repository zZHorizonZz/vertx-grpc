package io.vertx.mcp.bridge.grpc;

import io.vertx.core.Vertx;
import io.vertx.grpc.server.Service;
import io.vertx.mcp.ModelContextProtocolService;
import io.vertx.mcp.bridge.grpc.impl.ModelContextProtocolBridgeImpl;

public interface ModelContextProtocolBridge extends Service {
  static ModelContextProtocolBridge create(Vertx vertx, ModelContextProtocolService service) {
    return new ModelContextProtocolBridgeImpl(vertx, service);
  }
}
