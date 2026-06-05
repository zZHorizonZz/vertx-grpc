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
package io.vertx.tests.server;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.server.GrpcCorsOptions;
import io.vertx.grpc.server.GrpcProtocol;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerOptions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class PreflightTest extends ServerTestBase {

  private static final String UNARY_PATH = "/" + UNARY.fullMethodName();

  private HttpClient client;

  private void startWith(GrpcServerOptions options) {
    GrpcServer server = GrpcServer.server(vertx, options);
    server.callHandler(UNARY, req -> req.response().end());
    startServer(server);
    client = vertx.createHttpClient();
  }

  private void options(TestContext should, String path, java.util.Map<CharSequence, String> headers, java.util.function.BiConsumer<TestContext, HttpClientResponse> checks) {
    client
      .request(HttpMethod.OPTIONS, 8080, "localhost", path)
      .compose(request -> {
        if (headers != null) {
          headers.forEach(request::putHeader);
        }
        request.send();
        return request.response();
      })
      .onComplete(should.asyncAssertSuccess(resp -> checks.accept(should, resp)));
  }

  @Test
  public void testKnownMethodAdvertisesContentTypes(TestContext should) {
    startWith(new GrpcServerOptions());
    options(should, UNARY_PATH, null, (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      String allow = resp.getHeader("Allow");
      ctx.assertNotNull(allow);
      ctx.assertTrue(allow.contains("POST"), allow);
      ctx.assertTrue(allow.contains("OPTIONS"), allow);
      // the registered TestPreflightInvoker also binds GET at this path, the verb union is advertised
      ctx.assertTrue(allow.contains("GET"), allow);
      String acceptPost = resp.getHeader("Accept-Post");
      ctx.assertNotNull(acceptPost);
      ctx.assertTrue(acceptPost.contains("application/grpc"), acceptPost);
      ctx.assertTrue(acceptPost.contains("application/grpc-web"), acceptPost);
      ctx.assertTrue(acceptPost.contains("application/grpc-web-text"), acceptPost);
      // the registered invoker contributes application/json for POST
      ctx.assertTrue(acceptPost.contains("application/json"), acceptPost);
    });
  }

  @Test
  public void testUnknownPathIs404(TestContext should) {
    startWith(new GrpcServerOptions());
    options(should, "/does.not.Exist/Method", null, (ctx, resp) -> ctx.assertEquals(404, resp.statusCode()));
  }

  @Test
  public void testDisabledProtocolNotAdvertised(TestContext should) {
    GrpcServerOptions options = new GrpcServerOptions()
      .removeEnabledProtocol(GrpcProtocol.WEB_TEXT)
      .removeEnabledProtocol(GrpcProtocol.TRANSCODING);
    startWith(options);
    options(should, UNARY_PATH, null, (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      String acceptPost = resp.getHeader("Accept-Post");
      ctx.assertNotNull(acceptPost);
      ctx.assertTrue(acceptPost.contains("application/grpc"), acceptPost);
      ctx.assertFalse(acceptPost.contains("application/grpc-web-text"), acceptPost);
      ctx.assertFalse(acceptPost.contains("application/json"), acceptPost);
    });
  }

  @Test
  public void testNoCorsHeadersWhenCorsDisabled(TestContext should) {
    startWith(new GrpcServerOptions());
    options(should, UNARY_PATH, Collections.singletonMap(io.vertx.core.http.HttpHeaders.ORIGIN, "https://example.com"), (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      ctx.assertNull(resp.getHeader("Access-Control-Allow-Origin"));
    });
  }

  @Test
  public void testCorsAllowedOrigin(TestContext should) {
    GrpcServerOptions options = new GrpcServerOptions().setCors(new GrpcCorsOptions()
      .setAllowedOrigins(Arrays.asList("https://example.com"))
      .setAllowCredentials(true)
      .setMaxAgeSeconds(600));
    startWith(options);
    options(should, UNARY_PATH, Collections.singletonMap(io.vertx.core.http.HttpHeaders.ORIGIN, "https://example.com"), (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      ctx.assertEquals("https://example.com", resp.getHeader("Access-Control-Allow-Origin"));
      ctx.assertEquals("true", resp.getHeader("Access-Control-Allow-Credentials"));
      ctx.assertEquals("600", resp.getHeader("Access-Control-Max-Age"));
      String allowMethods = resp.getHeader("Access-Control-Allow-Methods");
      ctx.assertNotNull(allowMethods);
      ctx.assertTrue(allowMethods.contains("POST"), allowMethods);
      String expose = resp.getHeader("Access-Control-Expose-Headers");
      ctx.assertNotNull(expose);
      ctx.assertTrue(expose.contains("grpc-status"), expose);
    });
  }

  @Test
  public void testCorsDisallowedOrigin(TestContext should) {
    GrpcServerOptions options = new GrpcServerOptions().setCors(new GrpcCorsOptions()
      .setAllowedOrigins(Arrays.asList("https://allowed.com")));
    startWith(options);
    options(should, UNARY_PATH, Collections.singletonMap(io.vertx.core.http.HttpHeaders.ORIGIN, "https://evil.com"), (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      ctx.assertNull(resp.getHeader("Access-Control-Allow-Origin"));
      // discovery headers are still present
      ctx.assertNotNull(resp.getHeader("Allow"));
    });
  }

  @Test
  public void testCorsWildcardWithCredentialsEchoesOrigin(TestContext should) {
    GrpcServerOptions options = new GrpcServerOptions().setCors(new GrpcCorsOptions()
      .setAllowedOrigins(Arrays.asList("*"))
      .setAllowCredentials(true));
    startWith(options);
    options(should, UNARY_PATH, Collections.singletonMap(io.vertx.core.http.HttpHeaders.ORIGIN, "https://example.com"), (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      // with credentials the wildcard must not be used, the origin is echoed instead
      ctx.assertEquals("https://example.com", resp.getHeader("Access-Control-Allow-Origin"));
    });
  }

  @Test
  public void testCorsWildcardWithoutCredentials(TestContext should) {
    GrpcServerOptions options = new GrpcServerOptions().setCors(new GrpcCorsOptions()
      .setAllowedOrigins(Arrays.asList("*")));
    startWith(options);
    options(should, UNARY_PATH, Collections.singletonMap(io.vertx.core.http.HttpHeaders.ORIGIN, "https://example.com"), (ctx, resp) -> {
      ctx.assertEquals(204, resp.statusCode());
      ctx.assertEquals("*", resp.getHeader("Access-Control-Allow-Origin"));
    });
  }
}
