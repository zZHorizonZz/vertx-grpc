package io.vertx.mcp.bridge.grpc.impl;

import io.netty.handler.codec.DecoderResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.WriteStream;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class ModelContextProtocolServerRequest extends HttpServerRequestInternal implements HttpServerRequest {
  private final HttpMethod method;
  private final String path;
  private final Buffer body;
  private final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
  private final MultiMap params = MultiMap.caseInsensitiveMultiMap();
  private final Set<Cookie> cookies = new HashSet<>();
  private final HttpServerResponse response;
  private final HttpConnection connection = new ModelContextProtocolConnection();
  private final Context context;
  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;
  private boolean ended = false;
  private boolean paused = false;

  ModelContextProtocolServerRequest(HttpMethod method, String path, Buffer body, Context context) {
    this.method = method;
    this.path = path;
    this.body = body;
    this.context = context;
    this.headers.add("Content-Type", "application/json");
    this.response = new ModelContextProtocolServerResponse();
  }

  @Override
  public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public HttpServerRequest handler(Handler<Buffer> handler) {
    this.dataHandler = handler;
    if (!paused && handler != null) {
      handler.handle(body);
      ended = true;
      if (endHandler != null) {
        endHandler.handle(null);
      }
    }
    return this;
  }

  @Override
  public HttpServerRequest pause() {
    paused = true;
    return this;
  }

  @Override
  public HttpServerRequest resume() {
    paused = false;
    if (dataHandler != null && !ended) {
      dataHandler.handle(body);
      ended = true;
      if (endHandler != null) {
        endHandler.handle(null);
      }
    }
    return this;
  }

  @Override
  public HttpServerRequest fetch(long amount) {
    return this;
  }

  @Override
  public HttpServerRequest endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    if (ended && endHandler != null) {
      endHandler.handle(null);
    }
    return this;
  }

  @Override
  public HttpVersion version() {
    return HttpVersion.HTTP_1_1;
  }

  @Override
  public HttpMethod method() {
    return method;
  }

  @Override
  public String scheme() {
    return "http";
  }

  @Override
  public String uri() {
    return path;
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String query() {
    return null;
  }

  @Override
  public HostAndPort authority() {
    return HostAndPort.create("localhost", 80);
  }

  @Override
  public long bytesRead() {
    return body != null ? body.length() : 0;
  }

  @Override
  public HttpServerResponse response() {
    return response;
  }

  @Override
  public MultiMap headers() {
    return headers;
  }

  @Override
  public String getHeader(String headerName) {
    return headers.get(headerName);
  }

  @Override
  public HttpServerRequest setParamsCharset(String charset) {
    return this;
  }

  @Override
  public String getParamsCharset() {
    return "UTF-8";
  }

  @Override
  public MultiMap params(boolean semicolonIsNormalChar) {
    return params;
  }

  @Override
  public SocketAddress remoteAddress() {
    return SocketAddress.inetSocketAddress(80, "127.0.0.1");
  }

  @Override
  public SocketAddress localAddress() {
    return SocketAddress.inetSocketAddress(80, "127.0.0.1");
  }

  @Override
  public String absoluteURI() {
    return "http://localhost" + path;
  }

  @Override
  public Future<Buffer> body() {
    return Future.succeededFuture(body);
  }

  @Override
  public Future<Void> end() {
    ended = true;
    return Future.succeededFuture();
  }

  @Override
  public Future<NetSocket> toNetSocket() {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public HttpServerRequest setExpectMultipart(boolean expect) {
    return this;
  }

  @Override
  public boolean isExpectMultipart() {
    return false;
  }

  @Override
  public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
    return this;
  }

  @Override
  public MultiMap formAttributes() {
    return MultiMap.caseInsensitiveMultiMap();
  }

  @Override
  public String getFormAttribute(String attributeName) {
    return null;
  }

  @Override
  public Future<ServerWebSocket> toWebSocket() {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public boolean isEnded() {
    return ended;
  }

  @Override
  public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
    return this;
  }

  @Override
  public HttpConnection connection() {
    return connection;
  }

  @Override
  public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
    return this;
  }

  @Override
  public Cookie getCookie(String name) {
    return cookies.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
  }

  @Override
  public Cookie getCookie(String name, String domain, String path) {
    return cookies.stream()
      .filter(c -> c.getName().equals(name) &&
        Objects.equals(c.getDomain(), domain) &&
        Objects.equals(c.getPath(), path))
      .findFirst().orElse(null);
  }

  @Override
  public Set<Cookie> cookies(String name) {
    Set<Cookie> result = new HashSet<>();
    cookies.stream().filter(c -> c.getName().equals(name)).forEach(result::add);
    return Collections.unmodifiableSet(result);
  }

  @Override
  public Set<Cookie> cookies() {
    return cookies;
  }

  @Override
  public Pipe<Buffer> pipe() {
    return null;
  }

  @Override
  public Future<Void> pipeTo(WriteStream<Buffer> dst) {
    return Future.failedFuture("Not implemented");
  }

  @Override
  public DecoderResult decoderResult() {
    return DecoderResult.SUCCESS;
  }

  @Override
  public ContextInternal context() {
    return (ContextInternal) context;
  }

  @Override
  public Object metric() {
    return null;
  }
}

