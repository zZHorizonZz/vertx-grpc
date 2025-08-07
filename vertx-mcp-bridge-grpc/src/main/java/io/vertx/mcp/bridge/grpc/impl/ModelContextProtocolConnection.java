package io.vertx.mcp.bridge.grpc.impl;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.GoAway;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.SocketAddress;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ModelContextProtocolConnection implements HttpConnection {
  @Override
  public HttpConnection goAway(long errorCode, int lastStreamId, Buffer debugData) {
    return this;
  }

  @Override
  public HttpConnection goAwayHandler(Handler<GoAway> handler) {
    return this;
  }

  @Override
  public HttpConnection shutdownHandler(Handler<Void> handler) {
    return this;
  }

  @Override
  public Future<Void> shutdown() {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> shutdown(long timeout, TimeUnit unit) {
    return Future.succeededFuture();
  }

  @Override
  public HttpConnection closeHandler(Handler<Void> handler) {
    return this;
  }

  @Override
  public Future<Void> close() {
    return Future.succeededFuture();
  }

  @Override
  public Http2Settings remoteSettings() {
    return new Http2Settings();
  }

  @Override
  public Http2Settings settings() {
    return new Http2Settings();
  }

  @Override
  public Future<Void> updateSettings(Http2Settings settings) {
    return Future.succeededFuture();
  }

  @Override
  public HttpConnection remoteSettingsHandler(Handler<Http2Settings> handler) {
    return this;
  }

  @Override
  public Future<Buffer> ping(Buffer data) {
    return Future.succeededFuture(data);
  }

  @Override
  public HttpConnection pingHandler(Handler<Buffer> handler) {
    return this;
  }

  @Override
  public HttpConnection exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public SocketAddress remoteAddress() {
    return SocketAddress.inetSocketAddress(80, "127.0.0.1");
  }

  @Override
  public SocketAddress remoteAddress(boolean real) {
    return remoteAddress();
  }

  @Override
  public SocketAddress localAddress() {
    return SocketAddress.inetSocketAddress(80, "127.0.0.1");
  }

  @Override
  public SocketAddress localAddress(boolean real) {
    return localAddress();
  }

  @Override
  public boolean isSsl() {
    return false;
  }

  @Override
  public SSLSession sslSession() {
    return null;
  }

  @Override
  public List<Certificate> peerCertificates() throws SSLPeerUnverifiedException {
    return List.of();
  }

  @Override
  public String indicatedServerName() {
    return "";
  }
}
