package io.vertx.mcp.bridge.grpc.handler;

import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.PingRequest;
import io.vertx.jrpc.mcp.proto.PingResponse;

/**
 * Handler for the Ping RPC method.
 */
public class PingHandler extends BaseHandler<PingRequest, PingResponse> {

  public static final ServiceMethod<PingRequest, PingResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "Ping",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(PingRequest.newBuilder()));

  public PingHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<PingRequest, PingResponse> request) {
    request.handler(req -> request.response().end(PingResponse.getDefaultInstance()));
  }
}
