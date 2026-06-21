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
package io.vertx.grpc.it;

import io.grpc.examples.helloworld.GreeterGrpcClient;
import io.grpc.examples.helloworld.GreeterGrpcService;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.schema.ProtoSchema;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.core.Schemas;
import io.vertx.schema.protobuf.Proto;
import io.vertx.tests.common.GrpcTestBase;
import org.junit.Test;

/**
 * Cross-stack wire interoperability: a code-first, schema-defined {@code helloworld.Greeter}
 * service talks to the stock protoc-generated stack and vice versa, over the binary PROTOBUF wire
 * format - proving a schema-defined service is wire-compatible with regular protobuf/gRPC peers.
 * <p>
 * The schema below mirrors the {@code helloworld.proto} message shapes: {@code HelloRequest{name=1}}
 * and {@code HelloReply{message=1}}, on service {@code helloworld.Greeter} method {@code SayHello}.
 */
public class CodeFirstInteropTest extends GrpcTestBase {

  static final ServiceName GREETER = ServiceName.create("helloworld", "Greeter");

  static final ObjectSchema REQUEST = Schemas.object()
    .field("name", Proto.field(1, Schemas.string()));

  static final ObjectSchema REPLY = Schemas.object()
    .field("message", Proto.field(1, Schemas.string()));

  static final ServiceMethod<JsonObject, JsonObject> SCHEMA_SERVER = ProtoSchema.server(GREETER, "SayHello", REQUEST, REPLY);
  static final ServiceMethod<JsonObject, JsonObject> SCHEMA_CLIENT = ProtoSchema.client(GREETER, "SayHello", REQUEST, REPLY);

  @Test
  public void testSchemaServerProtocClient(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx).callHandler(SCHEMA_SERVER, call ->
      call.handler(request ->
        call.response().end(new JsonObject().put("message", "Hello " + request.getString("name")))));

    vertx.createHttpServer().requestHandler(server).listen(port, "localhost")
      .compose(v -> {
        GrpcClient client = GrpcClient.client(vertx);
        return client.request(SocketAddress.inetSocketAddress(port, "localhost"), GreeterGrpcClient.SayHello)
          .compose(request -> {
            request.end(HelloRequest.newBuilder().setName("World").build());
            return request.response().compose(GrpcClientResponse::last);
          });
      })
      .onComplete(should.asyncAssertSuccess(reply ->
        should.assertEquals("Hello World", reply.getMessage())));
  }

  @Test
  public void testProtocServerSchemaClient(TestContext should) {
    GrpcServer server = GrpcServer.server(vertx).callHandler(GreeterGrpcService.SayHello, call ->
      call.handler(helloRequest ->
        call.response().end(HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build())));

    vertx.createHttpServer().requestHandler(server).listen(port, "localhost")
      .compose(v -> {
        GrpcClient client = GrpcClient.client(vertx);
        return client.request(SocketAddress.inetSocketAddress(port, "localhost"), SCHEMA_CLIENT)
          .compose(request -> {
            request.end(new JsonObject().put("name", "World"));
            return request.response().compose(GrpcClientResponse::last);
          });
      })
      .onComplete(should.asyncAssertSuccess(reply ->
        should.assertEquals("Hello World", reply.getString("message"))));
  }
}
