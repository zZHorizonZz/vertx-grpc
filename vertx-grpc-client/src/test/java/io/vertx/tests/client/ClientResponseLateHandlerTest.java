/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.tests.client;

import io.grpc.stub.StreamObserver;
import io.vertx.core.Promise;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.tests.common.grpc.Empty;
import io.vertx.tests.common.grpc.Reply;
import io.vertx.tests.common.grpc.TestServiceGrpc;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * A response whose messages arrive before the application attaches its handlers.
 *
 * The application is entitled to attach the handlers after {@code request.response()} resolves, and nothing guarantees it does so before the server's messages arrive. On a loaded
 * machine the event loop delivers the whole response first, which is what makes this intermittent rather than constant.
 */
public class ClientResponseLateHandlerTest extends ClientTestBase {

  private static final int NUM_ITEMS = 200;

  private GrpcClient client;

  @Before
  public void setUp(TestContext should) {
    super.setUp(should);
    client = GrpcClient.client(vertx);
  }

  @Test
  public void testMessagesAreNotLostWhenHandlersAreAttachedLate(TestContext should) throws Exception {
    startServer(new TestServiceGrpc.TestServiceImplBase() {
      @Override
      public void source(Empty request, StreamObserver<Reply> responseObserver) {
        for (int i = 0; i < NUM_ITEMS; i++) {
          responseObserver.onNext(Reply.newBuilder().setMessage("the-value-" + i).build());
        }
        responseObserver.onCompleted();
      }
    });

    GrpcClientResponse<Empty, Reply> response = client
      .request(SocketAddress.inetSocketAddress(port, "localhost"), SOURCE)
      .compose(request -> {
        request.end(Empty.getDefaultInstance());
        return request.response();
      })
      .await(10, TimeUnit.SECONDS);

    // Wait for the response to be fully received before attaching anything. This is the state the
    // application finds itself in whenever it does not attach its handlers in the very same tick.
    response.end().await(10, TimeUnit.SECONDS);

    AtomicInteger received = new AtomicInteger();
    Promise<Integer> atEnd = Promise.promise();
    response.endHandler(v -> atEnd.tryComplete(received.get()));
    response.handler(reply -> received.incrementAndGet());

    assertEquals("every message must be delivered, and the end must come after them", NUM_ITEMS, (int) atEnd.future().await(10, TimeUnit.SECONDS));
  }
}
