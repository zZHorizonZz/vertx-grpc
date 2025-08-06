package io.vertx.jrpc.mcp;

import com.google.protobuf.Descriptors;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolBridge;
import io.vertx.jrpc.mcp.impl.ModelContextProtocolServiceImpl;
import io.vertx.jrpc.transcoding.impl.JrpcHttpHandler;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;
import io.vertx.jrpc.transcoding.model.JsonRpcResponse;
import io.vertx.tests.server.grpc.web.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for ModelContextProtocolBridge demonstrating full jrpc-mcp functionality.
 */
@RunWith(VertxUnitRunner.class)
public class ModelContextProtocolBridgeTest {

  public static GrpcMessageDecoder<Empty> EMPTY_DECODER = GrpcMessageDecoder.decoder(Empty.newBuilder());
  public static GrpcMessageEncoder<Empty> EMPTY_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageDecoder<EchoRequest> ECHO_REQUEST_DECODER = GrpcMessageDecoder.decoder(EchoRequest.newBuilder());
  public static GrpcMessageDecoder<EchoRequestBody> ECHO_REQUEST_BODY_DECODER = GrpcMessageDecoder.decoder(EchoRequestBody.newBuilder());
  public static GrpcMessageEncoder<EchoResponse> ECHO_RESPONSE_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageEncoder<EchoResponseBody> ECHO_RESPONSE_BODY_ENCODER = GrpcMessageEncoder.encoder();

  public static final ServiceName TEST_SERVICE_NAME = ServiceName.create(TestServiceGrpc.SERVICE_NAME);

  private Vertx vertx;
  private HttpServer httpServer;
  private HttpClient httpClient;
  private GrpcServer grpcServer;
  private ModelContextProtocolBridge bridge;
  private int port = 8080;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx();

    // Create gRPC server
    grpcServer = GrpcServer.server(vertx);

    // Create MCP service
    ModelContextProtocolServiceImpl mcpService = new ModelContextProtocolServiceImpl(vertx);
    TestService testService = new TestService();

    // Register the calculator service
    grpcServer.addService(testService);

    // Create and configure bridge
    new ModelContextProtocolBridge(vertx, mcpService).bind(grpcServer);

    // Create HTTP server with JSON-RPC handler
    httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port))
      .requestHandler(new JrpcHttpHandler(grpcServer));

    // Start server
    httpServer.listen().onComplete(ctx.asyncAssertSuccess());

    // Create HTTP client for testing
    httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port));
  }

  @After
  public void tearDown(TestContext ctx) {
    Async async = ctx.async();
    vertx.close().onComplete(ar -> async.complete());
  }

  @Test
  public void testBridgeRegistration(TestContext ctx) {
    Async async = ctx.async();

    // Test that bridge properly registers MCP service with gRPC server
    ctx.assertNotNull(bridge);
    ctx.assertNotNull(grpcServer);

    async.complete();
  }

  @Test
  public void testInitializeViaJsonRpc(TestContext ctx) {
    Async async = ctx.async();

    // Create JSON-RPC request for MCP initialize
    JsonRpcRequest request = new JsonRpcRequest(
      "Initialize",
      new JsonObject()
        .put("clientName", "Test Client")
        .put("clientVersion", "1.0.0")
        .put("capabilities", new JsonObject()),
      1
    );

    // Send request via HTTP
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertEquals("1", response.getId());
        ctx.assertTrue(response.isSuccess());

        JsonObject result = (JsonObject) response.getResult();
        ctx.assertEquals("Vert.x MCP Server", result.getString("serverName"));
        ctx.assertEquals("1.0.0", result.getString("serverVersion"));

        JsonObject capabilities = result.getJsonObject("capabilities");

        ctx.assertNotNull(capabilities.getJsonObject("tools"));
        ctx.assertNotNull(capabilities.getJsonObject("resources"));
        ctx.assertNotNull(capabilities.getJsonObject("prompts"));

        async.complete();
      }));
  }

  @Test
  public void testPingViaJsonRpc(TestContext ctx) {
    Async async = ctx.async();

    // Create JSON-RPC request for MCP ping
    JsonRpcRequest request = new JsonRpcRequest(
      "Ping",
      new JsonObject(),
      2
    );

    // Send request via HTTP
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertEquals("2", response.getId());
        ctx.assertTrue(response.isSuccess());

        JsonObject result = (JsonObject) response.getResult();
        ctx.assertTrue(result.containsKey("timestamp"));
        ctx.assertTrue(result.getLong("timestamp") > 0);

        async.complete();
      }));
  }

  @Test
  public void testToolsListViaJsonRpc(TestContext ctx) {
    Async async = ctx.async();

    // Create JSON-RPC request for MCP tools list
    JsonRpcRequest request = new JsonRpcRequest(
      "ToolsList",
      new JsonObject(),
      3
    );

    // Send request via HTTP
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertEquals("3", response.getId());
        ctx.assertTrue(response.isSuccess());

        JsonObject result = (JsonObject) response.getResult();
        ctx.assertTrue(result.containsKey("tools"));
        JsonArray tools = result.getJsonArray("tools");
        ctx.assertTrue(tools.size() > 0);

        // Check that our echo tool is listed
        boolean foundEchoTool = tools.stream()
          .map(JsonObject.class::cast)
          .anyMatch(tool -> "echo".equals(tool.getString("name")));
        ctx.assertTrue(foundEchoTool, "Echo tool should be in the tools list");

        async.complete();
      }));
  }

  @Test
  public void testToolsCallViaJsonRpc(TestContext ctx) {
    Async async = ctx.async();

    // Create JSON-RPC request for MCP tools call
    JsonObject parameters = new JsonObject()
      .put("toolId", "echo")
      .put("parameters", new JsonObject().put("message", "Hello World"));

    JsonRpcRequest request = new JsonRpcRequest(
      "ToolsCall",
      parameters,
      4
    );

    // Send request via HTTP
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertEquals("4", response.getId());
        ctx.assertTrue(response.isSuccess());

        JsonObject result = (JsonObject) response.getResult();
        ctx.assertTrue(result.getBoolean("success"));
        ctx.assertTrue(result.containsKey("result"));

        async.complete();
      }));
  }

  /*@Test
  public void testHttpClientToolExecution(TestContext ctx) {
    Async async = ctx.async();

    // Test that HTTP client tool can make calls to itself
    ModelContextProtocolTool httpTool = bridge.createBridgeClientTool(
      "test-http-tool",
      "Ping",
      "Test HTTP client tool"
    );

    JsonObject parameters = new JsonObject();

    httpTool.apply(parameters)
      .onComplete(ctx.asyncAssertSuccess(result -> {
        ctx.assertNotNull(result);
        ctx.assertTrue(result.containsKey("timestamp"));
        ctx.assertTrue(result.getLong("timestamp") > 0);

        async.complete();
      }));
  }

  @Test
  public void testBridgeWithoutHttpClient(TestContext ctx) {
    Async async = ctx.async();

    // Create bridge without HTTP client configuration
    ModelContextProtocolServiceImpl mcpService = new ModelContextProtocolServiceImpl(vertx);
    ModelContextProtocolBridge bridgeWithoutHttp = new ModelContextProtocolBridge(vertx, mcpService);

    ModelContextProtocolTool tool = bridgeWithoutHttp.createBridgeClientTool(
      "test-tool",
      "Ping",
      "Test tool"
    );

    // Should fail because HTTP client is not configured
    tool.apply(new JsonObject())
      .onComplete(ctx.asyncAssertFailure(error -> {
        ctx.assertTrue(error.getMessage().contains("HTTP client not configured"));
        async.complete();
      }));
  }*/

  private Future<JsonRpcResponse> sendJsonRpcRequest(JsonRpcRequest request) {
    return httpClient.request(HttpMethod.POST, "/io.modelcontextprotocol.ModelContextProtocolService")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(request.toJson().toBuffer());
      })
      .compose(HttpClientResponse::body)
      .map(buffer -> JsonRpcResponse.fromJson(buffer.toJsonObject()));
  }

  private static class TestService implements Service {

    @Override
    public ServiceName name() {
      return TEST_SERVICE_NAME;
    }

    @Override
    public Descriptors.ServiceDescriptor descriptor() {
      return Web.getDescriptor().findServiceByName("TestService");
    }

    @Override
    public void bind(GrpcServer server) {
      server.callHandler(new TestServiceMethod(), request -> {
        request.handler(requestBody -> request.response().end(EchoResponse.newBuilder().setPayload("Hello " + requestBody.getPayload()).build()));
      });
    }
  }

  private static class TestServiceMethod implements ServiceMethod<EchoRequest, EchoResponse> {

    @Override
    public ServiceName serviceName() {
      return TEST_SERVICE_NAME;
    }

    @Override
    public String methodName() {
      return "UnaryCall";
    }

    @Override
    public GrpcMessageEncoder<EchoResponse> encoder() {
      return ECHO_RESPONSE_ENCODER;
    }

    @Override
    public GrpcMessageDecoder<EchoRequest> decoder() {
      return ECHO_REQUEST_DECODER;
    }
  }
}
