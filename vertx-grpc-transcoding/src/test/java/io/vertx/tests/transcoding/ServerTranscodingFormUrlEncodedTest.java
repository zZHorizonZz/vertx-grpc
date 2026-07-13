package io.vertx.tests.transcoding;

import io.vertx.core.MultiMap;
import io.vertx.core.http.*;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerOptions;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpc.transcoding.MethodTranscodingOptions;
import io.vertx.grpc.transcoding.TranscodingServiceMethod;
import io.vertx.tests.common.GrpcTestBase;
import io.vertx.tests.server.grpc.web.EchoRequest;
import io.vertx.tests.server.grpc.web.EchoRequestBody;
import io.vertx.tests.server.grpc.web.EchoResponse;
import org.junit.Test;

import static io.vertx.tests.transcoding.ServerTranscodingTest.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServerTranscodingFormUrlEncodedTest extends GrpcTestBase {

  private static final String CONTENT_TYPE = HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString();
  private static final MultiMap HEADERS = HttpHeaders.headers()
    .add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE)
    .add(HttpHeaders.ACCEPT, "application/json")
    .copy(false);

  private static final MethodTranscodingOptions UNARY_TRANSCODING = new MethodTranscodingOptions()
    .setHttpMethod(HttpMethod.POST)
    .setPath("/hello")
    .setBody("*");

  private static final TranscodingServiceMethod<EchoRequest, EchoResponse> UNARY_CALL = TranscodingServiceMethod.server(
    TEST_SERVICE_NAME,
    "UnaryCall",
    ECHO_RESPONSE_ENCODER,
    ECHO_REQUEST_DECODER,
    UNARY_TRANSCODING
  );

  private static final MethodTranscodingOptions BODY_TRANSCODING = new MethodTranscodingOptions()
    .setHttpMethod(HttpMethod.POST)
    .setPath("/hello-body")
    .setBody("request");

  private static final TranscodingServiceMethod<EchoRequestBody, EchoResponse> BODY_CALL = TranscodingServiceMethod.server(
    TEST_SERVICE_NAME,
    "UnaryCallBody",
    ECHO_RESPONSE_ENCODER,
    GrpcMessageDecoder.decoder(EchoRequestBody.newBuilder()),
    BODY_TRANSCODING
  );

  private HttpClient httpClient;
  private HttpServer httpServer;

  @Override
  public void tearDown(TestContext should) {
    if (httpServer != null) {
      httpServer.close().onComplete(should.asyncAssertSuccess());
    }
    if (httpClient != null) {
      httpClient.close().onComplete(should.asyncAssertSuccess());
    }
    super.tearDown(should);
  }

  private void startServer(TestContext should) {
    httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port).setProtocolVersion(HttpVersion.HTTP_2));
    GrpcServer grpcServer = GrpcServer.server(vertx, new GrpcServerOptions());
    grpcServer.callHandler(UNARY_CALL, request -> request.handler(requestMsg -> {
      GrpcServerResponse<EchoRequest, EchoResponse> response = request.response();
      // Echo the payload, appending the repeated keys so both can be asserted.
      String payload = requestMsg.getPayload();
      if (!requestMsg.getKeysList().isEmpty()) {
        payload = payload + ":" + String.join(",", requestMsg.getKeysList());
      }
      response.end(EchoResponse.newBuilder().setPayload(payload).build());
    }));
    grpcServer.callHandler(BODY_CALL, request -> request.handler(requestMsg -> {
      GrpcServerResponse<EchoRequestBody, EchoResponse> response = request.response();
      response.end(EchoResponse.newBuilder().setPayload(requestMsg.getRequest().getPayload()).build());
    }));
    httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port)).requestHandler(grpcServer);
    httpServer.listen().onComplete(should.asyncAssertSuccess());
  }

  private void post(TestContext should, String path, String body, String expectedPayload) {
    httpClient.request(HttpMethod.POST, path).compose(req -> {
      req.headers().addAll(HEADERS);
      req.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length()));
      return req.send(body).compose(response -> response.body().map(response));
    }).onComplete(should.asyncAssertSuccess(response -> should.verify(v -> {
      assertEquals("expected 200, got " + response.statusCode(), 200, response.statusCode());
      assertEquals(expectedPayload, new io.vertx.core.json.JsonObject(response.body().result().toString()).getString("payload"));
    })));
  }

  @Test
  public void testSimpleField(TestContext should) {
    startServer(should);
    post(should, "/hello", "payload=foobar", "foobar");
  }

  @Test
  public void testPlusDecodedToSpace(TestContext should) {
    startServer(should);
    post(should, "/hello", "payload=hello+world", "hello world");
  }

  @Test
  public void testPercentEncodedValue(TestContext should) {
    startServer(should);
    post(should, "/hello", "payload=a%26b%3Dc", "a&b=c");
  }

  @Test
  public void testRepeatedField(TestContext should) {
    startServer(should);
    post(should, "/hello", "payload=p&keys=a&keys=b", "p:a,b");
  }

  @Test
  public void testBodyFieldPath(TestContext should) {
    startServer(should);
    post(should, "/hello-body", "payload=nested", "nested");
  }

  @Test
  public void testUnknownFieldRejected(TestContext should) {
    startServer(should);
    String body = "payload=ok&unknown=42";
    httpClient.request(HttpMethod.POST, "/hello").compose(req -> {
      req.headers().addAll(HEADERS);
      req.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length()));
      return req.send(body).compose(response -> response.body().map(response));
    }).onComplete(should.asyncAssertSuccess(response -> should.verify(v -> assertTrue("expected non-2xx, got " + response.statusCode(), response.statusCode() >= 400))));
  }

  @Test
  public void testUnknownPathRejected(TestContext should) {
    startServer(should);
    String body = "payload=ok";
    httpClient.request(HttpMethod.POST, "/nope").compose(req -> {
      req.headers().addAll(HEADERS);
      req.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length()));
      return req.send(body).compose(response -> response.body().map(response));
    }).onComplete(should.asyncAssertSuccess(response -> should.verify(v -> assertTrue("expected non-2xx, got " + response.statusCode(), response.statusCode() >= 400))));
  }
}
