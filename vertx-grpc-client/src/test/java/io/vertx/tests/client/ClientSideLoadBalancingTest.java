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
package io.vertx.tests.client;

import io.grpc.*;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcReadStream;
import io.vertx.tests.common.grpc.Reply;
import io.vertx.tests.common.grpc.Request;
import io.vertx.tests.common.grpc.TestServiceGrpc;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClientSideLoadBalancingTest extends ClientTestBase {

  private GrpcClient client;

  @Test
  public void testRoundRobin(TestContext should) throws Exception {

    int numServers = 3;
    List<SocketAddress> endpoints = new ArrayList<>();

    for (int i = 0;i < numServers;i++) {
      int idx = i;
      TestServiceGrpc.TestServiceImplBase called = new TestServiceGrpc.TestServiceImplBase() {
        @Override
        public void unary(Request request, StreamObserver<Reply> plainResponseObserver) {
          ServerCallStreamObserver<Reply> responseObserver =
            (ServerCallStreamObserver<Reply>) plainResponseObserver;
          responseObserver.onNext(Reply.newBuilder().setMessage("Hello " + request.getName() + idx).build());
          responseObserver.onCompleted();
        }
      };
      startServer(called, ServerBuilder.forPort(port + i));
      endpoints.add(SocketAddress.inetSocketAddress(port + i, "localhost"));
    }

    client = GrpcClient.builder(vertx)
      .withAddressResolver(AddressResolver.mappingResolver(address -> endpoints))
      .build();

    int numRequests = 10;

    List<String> replies = new ArrayList<>();
    for (int i = 0;i < numRequests;i++) {
      Reply reply = client.request(SocketAddress.inetSocketAddress(port, "localhost"), UNARY)
        .compose(req -> req
          .send(Request.newBuilder().setName("Julien").build())
          .compose(GrpcReadStream::last)).toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
      replies.add(reply.getMessage());
    }

    List<String> expected = IntStream
      .range(0, numRequests).mapToObj(idx -> "Hello Julien" + (idx % numServers))
      .collect(Collectors.toList());
    should.assertEquals(expected, replies);
  }
}
