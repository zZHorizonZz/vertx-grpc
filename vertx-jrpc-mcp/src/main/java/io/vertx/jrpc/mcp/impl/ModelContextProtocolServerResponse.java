package io.vertx.jrpc.mcp.impl;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.HostAndPort;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Set;

public class ModelContextProtocolServerResponse implements HttpServerResponse {
  private final Promise<Buffer> responsePromise = Promise.promise();
  private final Buffer responseBuffer = Buffer.buffer();
  private final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
  private final MultiMap trailers = MultiMap.caseInsensitiveMultiMap();
  private int statusCode = 200;
  private String statusMessage = "OK";
  private boolean chunked = false;
  private boolean ended = false;
  private boolean headWritten = false;
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> drainHandler;
  private Handler<Void> closeHandler;
  private Handler<Void> endHandler;
  private Handler<Void> headersEndHandler;
  private Handler<Void> bodyEndHandler;
  private long bytesWritten = 0;

  @Override
  public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
    return this;
  }

  @Override
  public HttpServerResponse drainHandler(Handler<Void> handler) {
    this.drainHandler = handler;
    return this;
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public HttpServerResponse setStatusCode(int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  @Override
  public String getStatusMessage() {
    return statusMessage;
  }

  @Override
  public HttpServerResponse setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
    return this;
  }

  @Override
  public HttpServerResponse setChunked(boolean chunked) {
    this.chunked = chunked;
    return this;
  }

  @Override
  public boolean isChunked() {
    return chunked;
  }

  @Override
  public MultiMap headers() {
    return headers;
  }

  @Override
  public HttpServerResponse putHeader(String name, String value) {
    headers.add(name, value);
    return this;
  }

  @Override
  public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
    headers.add(name.toString(), value.toString());
    return this;
  }

  @Override
  public HttpServerResponse putHeader(String name, Iterable<String> values) {
    for (String value : values) {
      headers.add(name, value);
    }
    return this;
  }

  @Override
  public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
    for (CharSequence value : values) {
      headers.add(name.toString(), value.toString());
    }
    return this;
  }

  @Override
  public MultiMap trailers() {
    return trailers;
  }

  @Override
  public HttpServerResponse putTrailer(String name, String value) {
    trailers.add(name, value);
    return this;
  }

  @Override
  public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
    trailers.add(name.toString(), value.toString());
    return this;
  }

  @Override
  public HttpServerResponse putTrailer(String name, Iterable<String> values) {
    for (String value : values) {
      trailers.add(name, value);
    }
    return this;
  }

  @Override
  public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> values) {
    for (CharSequence value : values) {
      trailers.add(name.toString(), value.toString());
    }
    return this;
  }

  @Override
  public HttpServerResponse closeHandler(Handler<Void> handler) {
    this.closeHandler = handler;
    return this;
  }

  @Override
  public HttpServerResponse endHandler(Handler<Void> handler) {
    this.endHandler = handler;
    return this;
  }

  @Override
  public Future<Void> writeHead() {
    headWritten = true;
    if (headersEndHandler != null) {
      headersEndHandler.handle(null);
    }
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> write(Buffer data) {
    responseBuffer.appendBuffer(data);
    bytesWritten += data.length();
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> write(String chunk, String enc) {
    Buffer buffer = Buffer.buffer(chunk, enc);
    return write(buffer);
  }

  @Override
  public Future<Void> write(String chunk) {
    return write(chunk, "UTF-8");
  }

  @Override
  public Future<Void> writeContinue() {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> writeEarlyHints(MultiMap headers) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> end(String chunk) {
    return write(chunk).compose(v -> end());
  }

  @Override
  public Future<Void> end(String chunk, String enc) {
    return write(chunk, enc).compose(v -> end());
  }

  @Override
  public Future<Void> end(Buffer chunk) {
    return write(chunk).compose(v -> end());
  }

  @Override
  public Future<Void> end() {
    if (!ended) {
      ended = true;
      responsePromise.complete(responseBuffer);
      if (bodyEndHandler != null) {
        bodyEndHandler.handle(null);
      }
      if (endHandler != null) {
        endHandler.handle(null);
      }
    }
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> sendFile(String filename, long offset, long length) {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public Future<Void> sendFile(FileChannel channel, long offset, long length) {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public Future<Void> sendFile(RandomAccessFile file, long offset, long length) {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public boolean ended() {
    return ended;
  }

  @Override
  public boolean closed() {
    return false;
  }

  @Override
  public boolean headWritten() {
    return headWritten;
  }

  @Override
  public HttpServerResponse headersEndHandler(Handler<Void> handler) {
    this.headersEndHandler = handler;
    return this;
  }

  @Override
  public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
    this.bodyEndHandler = handler;
    return this;
  }

  @Override
  public long bytesWritten() {
    return bytesWritten;
  }

  @Override
  public int streamId() {
    return -1;
  }

  @Override
  public Future<HttpServerResponse> push(HttpMethod method, HostAndPort authority, String path, MultiMap headers) {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public Future<Void> reset(long code) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> writeCustomFrame(int type, int flags, Buffer payload) {
    return Future.failedFuture("Not implemented in mock");
  }

  @Override
  public HttpServerResponse addCookie(Cookie cookie) {
    return this;
  }

  @Override
  public Cookie removeCookie(String name, boolean invalidate) {
    return null;
  }

  @Override
  public Set<Cookie> removeCookies(String name, boolean invalidate) {
    return Collections.emptySet();
  }

  @Override
  public Cookie removeCookie(String name, String domain, String path, boolean invalidate) {
    return null;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  public Future<Buffer> getResponseFuture() {
    return responsePromise.future();
  }
}
