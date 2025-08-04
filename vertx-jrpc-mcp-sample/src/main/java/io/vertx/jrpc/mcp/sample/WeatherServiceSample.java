package io.vertx.jrpc.mcp.sample;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolBridge;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.transcoding.impl.JrpcHttpHandler;

/**
 * Sample application demonstrating the Weather Service integration with MCP Bridge.
 *
 * This sample shows how to:
 * 1. Create a custom gRPC service (WeatherService)
 * 2. Integrate it with the Model Context Protocol Bridge
 * 3. Expose it via JSON-RPC over HTTP
 *
 * The weather service provides two main operations:
 * - GetAlerts: Retrieve weather alerts for a location
 * - GetForecast: Get weather forecast for a location
 */
public class WeatherServiceSample {

  private static final int PORT = 8080;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    // Create gRPC server
    GrpcServer grpcServer = GrpcServer.server(vertx);

    // Create and register the weather service
    WeatherServiceImpl weatherService = new WeatherServiceImpl();
    grpcServer.addService(weatherService);

    // Create MCP service
    ModelContextProtocolServiceImpl mcpService = new ModelContextProtocolServiceImpl(vertx);

    // Create and configure MCP bridge
    ModelContextProtocolBridge bridge = new ModelContextProtocolBridge(vertx, mcpService)
      .bind(grpcServer)
      .withHttpClient(PORT);

    // Create HTTP server with JSON-RPC handler
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(PORT))
      .requestHandler(new JrpcHttpHandler(grpcServer));

    // Start the server
    httpServer.listen()
      .onSuccess(server -> {
        System.out.println("Weather Service Sample started on port " + PORT);
        System.out.println();
        System.out.println("Available endpoints:");
        System.out.println("- MCP Initialize: POST /io.modelcontextprotocol.ModelContextProtocolService");
        System.out.println("- Weather Alerts: POST /io.vertx.jrpc.mcp.sample.weather.WeatherService");
        System.out.println("- Weather Forecast: POST /io.vertx.jrpc.mcp.sample.weather.WeatherService");
        System.out.println();
        System.out.println("Example JSON-RPC requests:");
        System.out.println();
        System.out.println("1. Initialize MCP:");
        System.out.println("{");
        System.out.println("  \"jsonrpc\": \"2.0\",");
        System.out.println("  \"method\": \"Initialize\",");
        System.out.println("  \"params\": {");
        System.out.println("    \"clientName\": \"Weather Client\",");
        System.out.println("    \"clientVersion\": \"1.0.0\",");
        System.out.println("    \"capabilities\": {}");
        System.out.println("  },");
        System.out.println("  \"id\": \"1\"");
        System.out.println("}");
        System.out.println();
        System.out.println("2. Get Weather Alerts:");
        System.out.println("{");
        System.out.println("  \"jsonrpc\": \"2.0\",");
        System.out.println("  \"method\": \"GetAlerts\",");
        System.out.println("  \"params\": {");
        System.out.println("    \"location\": \"New York\",");
        System.out.println("    \"severityLevel\": \"moderate\"");
        System.out.println("  },");
        System.out.println("  \"id\": \"2\"");
        System.out.println("}");
        System.out.println();
        System.out.println("3. Get Weather Forecast:");
        System.out.println("{");
        System.out.println("  \"jsonrpc\": \"2.0\",");
        System.out.println("  \"method\": \"GetForecast\",");
        System.out.println("  \"params\": {");
        System.out.println("    \"location\": \"London\",");
        System.out.println("    \"days\": 5");
        System.out.println("  },");
        System.out.println("  \"id\": \"3\"");
        System.out.println("}");
        System.out.println();
        System.out.println("Use curl or any HTTP client to test:");
        System.out.println("curl -X POST http://localhost:" + PORT + "/io.vertx.jrpc.mcp.sample.weather.WeatherService \\");
        System.out.println("  -H \"Content-Type: application/json\" \\");
        System.out.println("  -d '{\"jsonrpc\":\"2.0\",\"method\":\"GetForecast\",\"params\":{\"location\":\"Paris\",\"days\":3},\"id\":\"1\"}'");
      })
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
