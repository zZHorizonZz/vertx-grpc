package examples;

import examples.grpc.*;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.docgen.Source;
import io.vertx.grpc.common.*;
import io.vertx.grpc.health.HealthService;
import io.vertx.grpc.reflection.ReflectionService;
import io.vertx.grpc.server.*;
import io.vertx.grpc.transcoding.TranscodingServiceMethod;

@Source
public class GrpcServerExamples {

  public void createServer(Vertx vertx, HttpServerOptions options) {

    GrpcServer grpcServer = GrpcServer.server(vertx);

    HttpServer server = vertx.createHttpServer(options);

    server
      .requestHandler(grpcServer)
      .listen();
  }

  public void createServiceMethod() {
    ServiceName serviceName = ServiceName.create("examples.grpc", "Greeter");
    ServiceMethod<HelloRequest, HelloReply> sayHello = ServiceMethod.server(
      serviceName,
      "SayHello",
      GrpcMessageEncoder.encoder(),
      GrpcMessageDecoder.decoder(HelloRequest.newBuilder()));
  }

  public void reuseServiceMethod() {
    ServiceMethod<HelloRequest, HelloReply> sayHello = GreeterGrpcService.SayHello;
  }

  public void requestResponse(GrpcServer server) {

    server.callHandler(GreeterGrpcService.SayHello, request -> {

      request.handler(hello -> {

        GrpcServerResponse<HelloRequest, HelloReply> response = request.response();

        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + hello.getName()).build();

        response.end(reply);
      });
    });
  }

  public void streamingRequest(GrpcServer server) {

    server.callHandler(StreamingGrpcService.Sink, request -> {
      request.handler(item -> {
        // Process item
      });
      request.endHandler(v ->{
        // No more items
        // Send the response
        request.response().end(Empty.getDefaultInstance());
      });
      request.exceptionHandler(err -> {
        // Something wrong happened
      });
    });
  }

  public void streamingResponse(GrpcServer server) {

    server.callHandler(StreamingGrpcService.Source, request -> {
      GrpcServerResponse<Empty, Item> response = request.response();
      request.handler(empty -> {
        for (int i = 0;i < 10;i++) {
          response.write(Item.newBuilder().setValue("1").build());
        }
        response.end();
      });
    });
  }

  public void bidi(GrpcServer server) {

    server.callHandler(StreamingGrpcService.Pipe, request -> {

      request.handler(item -> request.response().write(item));
      request.endHandler(v -> request.response().end());
    });
  }

  public void disablingGrpcWeb(Vertx vertx) {
    GrpcServer server = GrpcServer.server(vertx, new GrpcServerOptions()
      .removeEnabledProtocol(GrpcProtocol.WEB)
      .removeEnabledProtocol(GrpcProtocol.WEB_TEXT)
    );
  }

  public void requestFlowControl(Vertx vertx, GrpcServerRequest<Item, Empty> request, Item item) {
    // Pause the response
    request.pause();

    performAsyncOperation().onComplete(ar -> {
      // And then resume
      request.resume();
    });
  }

  private Future<Buffer> performAsyncOperation() {
    return Future.succeededFuture();
  }

  private Future<Buffer> performAsyncOperation(Object o) {
    return Future.succeededFuture();
  }

  private Future<GrpcMessage> handleGrpcMessage(GrpcMessage o) {
    return Future.succeededFuture();
  }

  public void responseFlowControl(GrpcServerResponse<Empty, Item> response, Item item) {
    if (response.writeQueueFull()) {
      response.drainHandler(v -> {
        // Writable again
      });
    } else {
      response.write(item);
    }
  }

  public void checkTimeout(GrpcServerRequest<Empty, Item> request) {

    long timeout = request.timeout();

    if (timeout > 0L) {
      // A timeout has been received
    }
  }

  public void deadlineConfiguration(Vertx vertx) {
    GrpcServer server = GrpcServer.server(vertx, new GrpcServerOptions()
      .setScheduleDeadlineAutomatically(true)
      .setDeadlinePropagation(true)
    );
  }

  public void anemicJson(GrpcServer server) {
    ServiceMethod<JsonObject, JsonObject> sayHello = ServiceMethod.server(
      ServiceName.create("helloworld", "Greeter"),
      "SayHello",
      GrpcMessageEncoder.JSON_OBJECT,
      GrpcMessageDecoder.JSON_OBJECT
    );

    server.callHandler(sayHello, request -> {
      request.last().onSuccess(helloRequest -> {
        request.response().end(new JsonObject().put("message", "Hello " + helloRequest.getString("name")));
      });
    });
  }

  public void responseCompression(GrpcServerResponse<Empty, Item> response) {
    response.encoding("gzip");

    // Write items after encoding has been defined
    response.write(Item.newBuilder().setValue("item-1").build());
    response.write(Item.newBuilder().setValue("item-2").build());
    response.write(Item.newBuilder().setValue("item-3").build());
  }

  public void protobufLevelAPI(GrpcServer server) {

    ServiceName greeterServiceName = ServiceName.create("helloworld", "Greeter");

    server.callHandler(request -> {

      if (request.serviceName().equals(greeterServiceName) && request.methodName().equals("SayHello")) {

        request.handler(protoHello -> {
          // Handle protobuf encoded hello
          performAsyncOperation(protoHello)
            .onSuccess(protoReply -> {
              // Reply with protobuf encoded reply
              request.response().end(protoReply);
            }).onFailure(err -> {
              request.response()
                .status(GrpcStatus.ABORTED)
                .end();
            });
        });
      } else {
        request.response()
          .status(GrpcStatus.NOT_FOUND)
          .end();
      }
    });
  }

  public void messageLevelAPI(GrpcServer server) {

    ServiceName greeterServiceName = ServiceName.create("helloworld", "Greeter");

    server.callHandler(request -> {

      if (request.serviceName().equals(greeterServiceName) && request.methodName().equals("SayHello")) {

        request.messageHandler(helloMessage -> {

          // Can be identity or gzip
          String helloEncoding = helloMessage.encoding();

          // Handle hello message
          handleGrpcMessage(helloMessage)
            .onSuccess(replyMessage -> {
              // Reply with reply message

              // Can be identity or gzip
              String replyEncoding = replyMessage.encoding();

              // Send the reply
              request.response().endMessage(replyMessage);
            }).onFailure(err -> {
              request.response()
                .status(GrpcStatus.ABORTED)
                .end();
            });
        });
      } else {
        request.response()
          .status(GrpcStatus.NOT_FOUND)
          .end();
      }
    });
  }

  public void unaryStub1(GrpcServer server) {
    GreeterService service = new GreeterService() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };
  }

  public void unaryStub2(GrpcServer server) {
    GreeterService service = new GreeterService() {
      @Override
      public void sayHello(HelloRequest request, Completable<HelloReply> response) {
        response.succeed(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };
  }

  public void unaryStub3(GreeterGrpcService service, GrpcServer server) {
    server.addService(service);
  }

  public void streamingRequestStub(GrpcServer server) {
    StreamingGrpcService service = new StreamingGrpcService() {
      @Override
      public void sink(ReadStream<Item> stream, Completable<Empty> response) {
        stream.handler(item -> {
          System.out.println("Process item " + item.getValue());
        });
        // Send response
        stream.endHandler(v -> response.succeed(Empty.getDefaultInstance()));
      }
    };
    server.addService(service);
  }

  private Future<ReadStream<Item>> streamOfItems() {
    throw new UnsupportedOperationException();
  }

  public void streamingResponseStub1() {
    StreamingService service = new StreamingService() {
      @Override
      public Future<ReadStream<Item>> source(Empty request) {
        return streamOfItems();
      }
    };
  }

  public void streamingResponseStub2() {
    StreamingService service = new StreamingService() {
      @Override
      public void source(Empty request, WriteStream<Item> response) {
        response.write(Item.newBuilder().setValue("value-1").build());
        response.end(Item.newBuilder().setValue("value-2").build());
      }
    };
  }

  public void reflectionExample(Vertx vertx, HttpServerOptions options) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    // Add reflection service
    grpcServer.addService(ReflectionService.v1());

    GreeterGrpcService greeterService = new GreeterGrpcService() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };

    grpcServer.addService(greeterService);

    // Start the server
    vertx.createHttpServer(options)
      .requestHandler(grpcServer)
      .listen();
  }

  public void healthServiceExample(Vertx vertx, HttpServerOptions options) {
    // Create a gRPC server
    GrpcServer grpcServer = GrpcServer.server(vertx);

    // Create a health service instance
    HealthService healthService = HealthService.create(vertx);

    // Register health checks for your services
    healthService.register("my.service.name", () -> Future.succeededFuture(true));

    // Add the health service to the gRPC server
    grpcServer.addService(healthService);

    // Start the server
    vertx.createHttpServer(options)
      .requestHandler(grpcServer)
      .listen();
  }
}
