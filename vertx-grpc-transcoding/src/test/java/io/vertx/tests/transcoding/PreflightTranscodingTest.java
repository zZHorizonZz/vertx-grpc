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

package io.vertx.tests.transcoding;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.transcoding.MethodTranscodingOptions;
import io.vertx.grpc.transcoding.TranscodingServiceMethod;
import io.vertx.tests.common.GrpcTestBase;
import io.vertx.tests.server.grpc.web.EchoRequest;
import io.vertx.tests.server.grpc.web.EchoResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.tests.transcoding.ServerTranscodingTest.ECHO_REQUEST_DECODER;
import static io.vertx.tests.transcoding.ServerTranscodingTest.ECHO_RESPONSE_ENCODER;
import static io.vertx.tests.transcoding.ServerTranscodingTest.TEST_SERVICE_NAME;

@RunWith(VertxUnitRunner.class)
public class PreflightTranscodingTest extends GrpcTestBase {

  private HttpClient client;
  private HttpServer httpServer;

  private void startServer(GrpcServer server, TestContext should) {
    httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port)).requestHandler(server);
    httpServer.listen().await();
    client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port));
  }

  @Override
  public void tearDown(TestContext should) {
    if (httpServer != null) {
      httpServer.close().await();
    }
    super.tearDown(should);
  }

  private static TranscodingServiceMethod<EchoRequest, EchoResponse> method(String name, HttpMethod httpMethod, String path, String body) {
    MethodTranscodingOptions options = new MethodTranscodingOptions().setHttpMethod(httpMethod).setPath(path);
    if (body != null) {
      options.setBody(body);
    }
    return TranscodingServiceMethod.server(TEST_SERVICE_NAME, name, ECHO_RESPONSE_ENCODER, ECHO_REQUEST_DECODER, options);
  }

  private void options(TestContext should, String path, java.util.function.BiConsumer<TestContext, io.vertx.core.http.HttpClientResponse> checks) {
    client.request(HttpMethod.OPTIONS, port, "localhost", path)
      .compose(request -> {
        request.send();
        return request.response();
      })
      .onComplete(should.asyncAssertSuccess(resp -> checks.accept(should, resp)));
  }

  @Test
  public void testMixedVerbsAndAcceptPost(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx);
    server.callHandler(method("GetWidget", HttpMethod.GET, "/widgets", null), req -> req.response().end());
    server.callHandler(method("CreateWidget", HttpMethod.POST, "/widgets", "*"), req -> req.response().end());
    startServer(server, should);

    options(should, "/widgets", (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      String allow = resp.getHeader("Allow");
      ctx.assertNotNull(allow);
      ctx.assertTrue(allow.contains("GET"), allow);
      ctx.assertTrue(allow.contains("POST"), allow);
      ctx.assertTrue(allow.contains("OPTIONS"), allow);
      // a POST binding exists at this path, application/json is accepted for POST
      String acceptPost = resp.getHeader("Accept-Post");
      ctx.assertNotNull(acceptPost);
      ctx.assertTrue(acceptPost.contains("application/json"), acceptPost);
    });
  }

  @Test
  public void testGetOnlyHasNoAcceptPost(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx);
    server.callHandler(method("GetItem", HttpMethod.GET, "/items/{id}", null), req -> req.response().end());
    startServer(server, should);

    options(should, "/items/123", (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      String allow = resp.getHeader("Allow");
      ctx.assertNotNull(allow);
      ctx.assertTrue(allow.contains("GET"), allow);
      ctx.assertTrue(allow.contains("OPTIONS"), allow);
      ctx.assertFalse(allow.contains("POST"), allow);
      // a GET-only resource accepts no POST body
      ctx.assertNull(resp.getHeader("Accept-Post"));
    });
  }

  @Test
  public void testCustomVerbIsAdvertised(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx);
    // A non-standard HTTP verb, outside the common GET/POST/PUT/DELETE/PATCH set
    server.callHandler(method("AclWidget", HttpMethod.valueOf("ACL"), "/widgets", "*"), req -> req.response().end());
    startServer(server, should);

    options(should, "/widgets", (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      String allow = resp.getHeader("Allow");
      ctx.assertNotNull(allow);
      ctx.assertTrue(allow.contains("ACL"), allow);
    });
  }

  @Test
  public void testUnknownPathIs404(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx);
    server.callHandler(method("GetItem", HttpMethod.GET, "/items/{id}", null), req -> req.response().end());
    startServer(server, should);

    options(should, "/nowhere", (ctx, resp) -> ctx.assertEquals(404, resp.statusCode()));
  }

  @Test
  public void testGrpcMethodPathAdvertisesGrpcProtocols(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx);
    server.callHandler(method("GetWidget", HttpMethod.GET, "/widgets", null), req -> req.response().end());
    startServer(server, should);

    // The transcoded method is also reachable at its full gRPC path, where the gRPC protocols bind
    options(should, "/" + TEST_SERVICE_NAME.fullyQualifiedName() + "/GetWidget", (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      ctx.assertTrue(resp.getHeader("Allow").contains("POST"), resp.getHeader("Allow"));
      String acceptPost = resp.getHeader("Accept-Post");
      ctx.assertNotNull(acceptPost);
      ctx.assertTrue(acceptPost.contains("application/grpc"), acceptPost);
      ctx.assertTrue(acceptPost.contains("application/grpc-web"), acceptPost);
    });
  }
}
