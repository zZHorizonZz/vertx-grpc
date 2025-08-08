package io.vertx.mcp.server.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.Future;
import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.mcp.ModelContextProtocolServer;
import io.vertx.jrpc.mcp.proto.InitializeRequest;
import io.vertx.jrpc.mcp.proto.InitializeResponse;

public class InitializeHandler extends BaseHandler<InitializeRequest, InitializeResponse> {

  public static final ServiceMethod<InitializeRequest, InitializeResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolServer"),
    "Initialize",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(InitializeRequest.newBuilder()));

  public InitializeHandler(GrpcServer server, ModelContextProtocolServer service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<InitializeRequest, InitializeResponse> request) {
    request.handler(req -> {
      try {
        initialize(req)
          .onSuccess(response -> request.response().end(response))
          .onFailure(err -> request.response().status(GrpcStatus.INTERNAL).end());
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }

  public Future<InitializeResponse> initialize(InitializeRequest request) {
    Struct.Builder capabilitiesBuilder = Struct.newBuilder();

    try {
      JsonFormat.parser().merge(service.getCapabilities().encode(), capabilitiesBuilder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    // Create response with server information
    InitializeResponse response = InitializeResponse.newBuilder()
      .setProtocolVersion(service.getProtocolVersion() != null ? service.getProtocolVersion() : request.getProtocolVersion())
      .setServerInfo(InitializeResponse.ServerInfo.newBuilder()
        .setName(service.getServerName())
        .setVersion(service.getServerVersion())
        .build()
      )
      .setCapabilities(capabilitiesBuilder.build())
      .build();

    return Future.succeededFuture(response);
  }
}
