/*
 * Copyright (c) 2011-2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.it;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.it.signature.GreetReply;
import io.vertx.grpc.it.signature.GreetRequest;
import io.vertx.grpc.it.signature.Kind;
import io.vertx.grpc.it.signature.PublishReply;
import io.vertx.grpc.it.signature.PublishRequest;
import io.vertx.grpc.it.signature.SendMessageReply;
import io.vertx.grpc.it.signature.SendMessageRequest;
import io.vertx.grpc.it.signature.SignatureClient;
import io.vertx.grpc.it.signature.SignatureGrpcClient;
import io.vertx.grpc.it.signature.SignatureGrpcService;
import io.vertx.grpc.it.signature.SignatureService;
import io.vertx.grpc.server.GrpcServer;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class MethodSignatureTest extends ProxyTestBase {

  private SignatureClient startServerAndClient(SignatureService service) throws Exception {
    GrpcServer grpcServer = GrpcServer.server(vertx);
    grpcServer.addService(SignatureGrpcService.of(service));
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(grpcServer)
      .listen(8080).toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);

    GrpcClient grpcClient = GrpcClient.client(vertx);
    return SignatureGrpcClient.create(grpcClient, SocketAddress.inetSocketAddress(port, "localhost"));
  }

  @Test
  public void testFlattenedScalarOverload(TestContext should) throws Exception {
    SignatureClient client = startServerAndClient(new SignatureService() {
      @Override
      public Future<GreetReply> greet(GreetRequest request) {
        return Future.succeededFuture(GreetReply.newBuilder()
          .setMessage("Hello " + request.getName())
          .build());
      }
    });

    Async test = should.async();
    // Flattened overload generated from method_signature = "name"
    client.greet("World").onComplete(should.asyncAssertSuccess(reply -> {
      should.assertEquals("Hello World", reply.getMessage());
      test.complete();
    }));
    test.awaitSuccess();
  }

  @Test
  public void testFlattenedMultiArgAndRepeatedOverloads(TestContext should) throws Exception {
    SignatureClient client = startServerAndClient(new SignatureService() {
      @Override
      public Future<SendMessageReply> sendMessage(SendMessageRequest request) {
        String ack = request.getTopic() + ":" + request.getBody();
        if (request.getTagsCount() > 0) {
          ack += "[" + String.join(",", request.getTagsList()) + "]";
        }
        return Future.succeededFuture(SendMessageReply.newBuilder().setAck(ack).build());
      }
    });

    Async test = should.async();
    // Overload 1: method_signature = "topic,body"
    client.sendMessage("news", "hello")
      .compose(reply -> {
        should.assertEquals("news:hello", reply.getAck());
        // Overload 2: method_signature = "topic,body,tags"
        return client.sendMessage("news", "hello", List.of("a", "b"));
      })
      .onComplete(should.asyncAssertSuccess(reply -> {
        should.assertEquals("news:hello[a,b]", reply.getAck());
        test.complete();
      }));
    test.awaitSuccess();
  }

  @Test
  public void testFlattenedEnumAndMapOverload(TestContext should) throws Exception {
    SignatureClient client = startServerAndClient(new SignatureService() {
      @Override
      public Future<PublishReply> publish(PublishRequest request) {
        // Render the headers deterministically.
        StringBuilder ack = new StringBuilder(request.getKind().name());
        Map<String, String> sorted = new TreeMap<>(request.getHeadersMap());
        for (Map.Entry<String, String> e : sorted.entrySet()) {
          ack.append('|').append(e.getKey()).append('=').append(e.getValue());
        }
        return Future.succeededFuture(PublishReply.newBuilder().setAck(ack.toString()).build());
      }
    });

    Async test = should.async();
    // Flattened overload generated from method_signature = "kind,headers" — kind is an enum,
    // headers is a map<string, string>.
    client.publish(Kind.KIND_WARN, Map.of("x", "1", "y", "2"))
      .onComplete(should.asyncAssertSuccess(reply -> {
        should.assertEquals("KIND_WARN|x=1|y=2", reply.getAck());
        test.complete();
      }));
    test.awaitSuccess();
  }

  // Compile-only assertion that the flattened overload is also generated for unary-input
  // server-streaming methods. If method_signature support stops emitting this overload, the
  // method-reference below will fail to compile.
  @SuppressWarnings("unused")
  private static final java.util.function.Function<SignatureClient, ?> SERVER_STREAMING_OVERLOAD_REFERENCE =
    client -> client.streamGreetings("World");
}
