package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.core.Handler;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolService;

/**
 * Base handler class for all MCP service handlers.
 *
 * @param <Req> the request type
 * @param <Resp> the response type
 */
public abstract class BaseHandler<Req, Resp> implements Handler<GrpcServerRequest<Req, Resp>> {

  protected final GrpcServer server;
  protected final ModelContextProtocolService service;

  /**
   * Creates a new base handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public BaseHandler(GrpcServer server, ModelContextProtocolService service) {
    this.server = server;
    this.service = service;
  }
}
