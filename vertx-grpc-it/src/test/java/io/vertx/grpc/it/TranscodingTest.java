package io.vertx.grpc.it;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.server.GrpcServer;
import org.junit.Test;

public class TranscodingTest extends ProxyTestBase {

  @Test
  public void testUnary01(TestContext should) {

    HttpClient client = vertx.createHttpClient();

    Future<HttpServer> server = vertx.createHttpServer().requestHandler(GrpcServer.server(vertx).callHandler(io.grpc.examples.helloworld.VertxGreeterGrpcServer.SayHello_JSON, call -> {
      call.handler(helloRequest -> {
        io.grpc.examples.helloworld.HelloReply helloReply = io.grpc.examples.helloworld.HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        call.response().end(helloReply);
      });
    })).listen(8080, "localhost");

    RequestOptions options = new RequestOptions().setHost("localhost").setPort(8080).setURI("/io.grpc.examples.helloworld.VertxGreeter/SayHello_JSON").setMethod(HttpMethod.POST);

    Async test = should.async();

    server.onComplete(should.asyncAssertSuccess(v -> {
      client.request(options).compose(req -> {
        req.putHeader("Content-Type", "application/json");
        req.putHeader("Accept", "application/json");
        req.write("{\"name\":\"Julien\"}");
        return req.send();
      }).compose(resp -> {
        should.assertEquals(200, resp.statusCode());
        should.assertEquals("application/json", resp.getHeader("Content-Type"));
        return resp.body();
      }).onComplete(should.asyncAssertSuccess(body -> {
        should.assertEquals("{\"message\":\"Hello Julien\"}", body.toString());
        test.complete();
      }));
    }));

    test.awaitSuccess(20_000);
  }
}
