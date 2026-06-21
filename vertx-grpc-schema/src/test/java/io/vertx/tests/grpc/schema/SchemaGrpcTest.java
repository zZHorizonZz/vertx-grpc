/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.tests.grpc.schema;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.schema.ProtoSchema;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.core.Schemas;
import io.vertx.schema.protobuf.Proto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * End-to-end test: a gRPC service whose request/response are defined purely by a code-first
 * schema (no .proto, no generated classes), exchanged as JsonObject over the real
 * GrpcServer/GrpcClient. Exercises both the PROTOBUF and JSON wire formats.
 */
@RunWith(VertxUnitRunner.class)
public class SchemaGrpcTest {

  static final ObjectSchema REQUEST = Schemas.object()
    .field("name", Proto.field(1, Schemas.string()));

  static final ObjectSchema REPLY = Schemas.object()
    .field("message", Proto.field(1, Schemas.string()));

  static final ServiceName GREETER = ServiceName.create("io.vertx.test", "Greeter");

  static final ServiceMethod<JsonObject, JsonObject> SERVER = ProtoSchema.server(GREETER, "SayHello", REQUEST, REPLY);
  static final ServiceMethod<JsonObject, JsonObject> CLIENT = ProtoSchema.client(GREETER, "SayHello", REQUEST, REPLY);

  Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext should) {
    vertx.close().onComplete(should.asyncAssertSuccess());
  }

  @Test
  public void sayHelloProtobuf(TestContext should) {
    sayHello(should, WireFormat.PROTOBUF);
  }

  @Test
  public void sayHelloJson(TestContext should) {
    sayHello(should, WireFormat.JSON);
  }

  private void sayHello(TestContext should, WireFormat format) {
    GrpcServer server = GrpcServer.server(vertx).callHandler(SERVER, call ->
      call.handler(request ->
        call.response().end(new JsonObject().put("message", "Hello " + request.getString("name")))));

    vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"))
      .requestHandler(server)
      .listen()
      .compose(v -> {
        GrpcClient client = GrpcClient.client(vertx);
        return client.request(SocketAddress.inetSocketAddress(8080, "localhost"), CLIENT)
          .compose(request -> {
            request.format(format);
            request.end(new JsonObject().put("name", "World"));
            return request.response().compose(GrpcClientResponse::last);
          });
      })
      .onComplete(should.asyncAssertSuccess(reply ->
        should.assertEquals("Hello World", reply.getString("message"))));
  }
}
