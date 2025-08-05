/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.jrpc.transcoding;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
import io.vertx.jrpc.transcoding.impl.JrpcHttpHandler;
import io.vertx.jrpc.transcoding.model.JsonRpcRequest;
import io.vertx.jrpc.transcoding.model.JsonRpcResponse;
import io.vertx.tests.server.grpc.web.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for the JSON-RPC transcoding module.
 */
@RunWith(VertxUnitRunner.class)
public class JrpcTranscodingTest {

  public static GrpcMessageDecoder<Empty> EMPTY_DECODER = GrpcMessageDecoder.decoder(Empty.newBuilder());
  public static GrpcMessageEncoder<Empty> EMPTY_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageDecoder<EchoRequest> ECHO_REQUEST_DECODER = GrpcMessageDecoder.decoder(EchoRequest.newBuilder());
  public static GrpcMessageDecoder<EchoRequestBody> ECHO_REQUEST_BODY_DECODER = GrpcMessageDecoder.decoder(EchoRequestBody.newBuilder());
  public static GrpcMessageEncoder<EchoResponse> ECHO_RESPONSE_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageEncoder<EchoResponseBody> ECHO_RESPONSE_BODY_ENCODER = GrpcMessageEncoder.encoder();

  public static final ServiceName TEST_SERVICE_NAME = ServiceName.create(TestServiceGrpc.SERVICE_NAME);

  private Vertx vertx;
  private GrpcServer server;
  private HttpClient client;
  private int port;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx();
    port = 8080;

    // Create a gRPC server
    server = GrpcServer.server(vertx);

    // Create a calculator service
    TestService testService = new TestService();

    // Register the calculator service
    server.callHandler(testService, request -> {
      request.handler(requestBody -> request.response().end(EchoResponse.newBuilder().setPayload("Hello " + requestBody.getPayload()).build()));
    });

    // Create an HTTP server and set the gRPC server as the request handler
    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port)).requestHandler(new JrpcHttpHandler(server));

    // Listen on the HTTP server
    httpServer.listen().onComplete(ctx.asyncAssertSuccess());

    // Create an HTTP client
    HttpClientOptions clientOptions = new HttpClientOptions().setDefaultPort(port);
    client = vertx.createHttpClient(clientOptions);
  }

  @After
  public void tearDown(TestContext ctx) {
    Async async = ctx.async();
    vertx.close().onComplete(ar -> async.complete());
  }

  @Test
  //@Ignore("We do not currently support positional parameters")
  public void testPositionalParams(TestContext ctx) {
    Async async = ctx.async();

    // Create a JSON-RPC request with positional parameters
    JsonRpcRequest request = new JsonRpcRequest(
      "UnaryCall",
      new JsonArray().add("Julien"),
      1
    );

    // Send the request
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertEquals("Hello Julien", ((JsonObject) response.getResult()).getString("payload"));
        ctx.assertEquals(1, response.getId());
        ctx.assertNull(response.getError());
        ctx.assertTrue(response.isSuccess());
        async.complete();
      }));

    async.awaitSuccess(5000);
  }

  @Test
  public void testNamedParams(TestContext ctx) {
    Async async = ctx.async();

    // Create a JSON-RPC request with named parameters
    JsonRpcRequest request = new JsonRpcRequest(
      "UnaryCall",
      new JsonObject().put("payload", "Julien"),
      2
    );

    // Send the request
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertEquals(JsonObject.class, response.getResult().getClass());
        ctx.assertEquals("Hello Julien", ((JsonObject) response.getResult()).getString("payload"));
        ctx.assertEquals(2, response.getId());
        ctx.assertNull(response.getError());
        ctx.assertTrue(response.isSuccess());
        async.complete();
      }));

    async.awaitSuccess(5000);
  }

  @Test
  @Ignore("The issue here is that we cannot at the moment specify how should request end")
  public void testMethodNotFound(TestContext ctx) {
    Async async = ctx.async();

    // Create a JSON-RPC request with a non-existent method
    JsonRpcRequest request = new JsonRpcRequest(
      "nonExistentMethod",
      new JsonArray().add(40).add(2),
      3
    );

    // Send the request
    sendJsonRpcRequest(request)
      .onComplete(ctx.asyncAssertSuccess(response -> {
        ctx.assertEquals("2.0", response.getJsonrpc());
        ctx.assertNull(response.getResult());
        ctx.assertEquals(3, response.getId());
        ctx.assertNotNull(response.getError());
        ctx.assertEquals(-32601, response.getError().getCode());
        ctx.assertEquals("Method not found", response.getError().getMessage());
        ctx.assertFalse(response.isSuccess());
        async.complete();
      }));

    async.awaitSuccess(5000);
  }

  private Future<JsonRpcResponse> sendJsonRpcRequest(JsonRpcRequest request) {
    return client.request(HttpMethod.POST, port, "localhost", "/" + TestServiceGrpc.SERVICE_NAME)
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(Buffer.buffer(request.toJson().encode()))
          .compose(resp -> {
            return resp.body().map(body -> {
              JsonObject json = new JsonObject(body);
              return JsonRpcResponse.fromJson(json);
            });
          });
      });
  }

  private static class TestService implements ServiceMethod<EchoRequest, EchoResponse> {

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
