package io.vertx.jrpc.mcp.sample;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.mcp.ModelContextProtocolOptions;
import io.vertx.mcp.bridge.grpc.ModelContextProtocolBridge;
import io.vertx.mcp.bridge.grpc.ModelContextProtocolHandler;
import io.vertx.mcp.impl.ModelContextProtocolServiceImpl;

/**
 * Sample application demonstrating the Weather Service integration with MCP Bridge.
 * <p>
 * The weather service provides two main operations: - GetAlerts: Retrieve weather alerts for a location - GetForecast: Get weather forecast for a location
 */
public class WeatherServiceSample {

  private static final int PORT = 8080;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    // Create gRPC server
    GrpcServer grpcServer = GrpcServer.server(vertx);

    // Create and register the weather service
    grpcServer.addService(new WeatherServiceImpl());
    grpcServer.addService(new PokemonServiceImpl());
    grpcServer.addService(new VertxDocumentationServiceImpl());

    // Create and configure MCP bridge
    ModelContextProtocolBridge.create(vertx, new ModelContextProtocolServiceImpl(new ModelContextProtocolOptions())).bind(grpcServer);

    // Create HTTP server with JSON-RPC handler
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(PORT))
      .requestHandler(new ModelContextProtocolHandler(grpcServer));

    // Start the server
    httpServer.listen()
      .onSuccess(server -> System.out.println("Weather Service Sample started on port " + PORT))
      .onFailure(error -> {
        System.err.println("Failed to start server: " + error.getMessage());
        vertx.close();
      });

    // Graceful shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down Weather Service Sample...");
      vertx.close();
    }));
  }
}
