package io.vertx.grpc.transcoding.impl;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.common.impl.GrpcMessageDeframer;
import io.vertx.grpc.common.impl.GrpcMessageFrame;
import io.vertx.grpc.server.GrpcProtocol;
import io.vertx.grpc.server.impl.HttpGrpcOutboundStream;

public class TranscodingGrpcOutboundStream extends HttpGrpcOutboundStream {

  private static final String SSE_MEDIA_TYPE = "text/event-stream";

  private Promise<Void> head;
  private final ContextInternal context;
  private final HttpServerResponse httpResponse;
  private final String transcodingResponseBody;
  private final boolean responseStreaming;

  public TranscodingGrpcOutboundStream(ContextInternal context, HttpServerRequest httpRequest,
                                       String transcodingResponseBody, boolean responseStreaming,
                                       GrpcMessageDeframer deframer) {
    super(httpRequest, GrpcProtocol.TRANSCODING, deframer);

    this.context = context;
    this.httpResponse = httpRequest.response();
    this.transcodingResponseBody = transcodingResponseBody;
    this.responseStreaming = responseStreaming;
  }

  @Override
  protected String contentType(WireFormat wireFormat) {
    switch (wireFormat) {
      case PROTOBUF:
        throw new UnsupportedOperationException();
      case JSON:
        // TODO: When WireFormat (or its successor) gains framing-mode configuration, drive
        // the streaming response framing from there so JSON-RPC and any other JSON-over-HTTP
        // protocols can share SSE / NDJSON / JSON-array choices instead of each transport
        // hard-coding its own. Today: SSE for server-streaming, plain JSON for unary.
        return responseStreaming ? SSE_MEDIA_TYPE : protocol.mediaType();
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  protected void encodeGrpcHeaders(MultiMap grpcHeaders, MultiMap httpHeaders, String encoding) {
  }

  @Override
  public Future<Void> writeEnd() {
    if (status != GrpcStatus.OK) {
      httpResponse.setStatusCode(GrpcTranscodingError.fromHttp2Code(status.code).getHttpStatusCode());
    }
    return super.writeEnd();
  }

  @Override
  public Future<Void> writeHead() {
    if (responseStreaming) {
      return super.writeHead();
    }
    head = context.promise();
    return head.future();
  }

  @Override
  public Future<Void> writeMessage(GrpcMessageFrame frame) {
    Buffer payload;
    try {
      payload = frame.message().payload();
    } catch (CodecException e) {
      return context.failedFuture(e);
    }
    Future<Void> res;
    try {
      Buffer transcoded = MessageWeaver.weaveResponseMessage(payload, transcodingResponseBody);
      if (responseStreaming) {
        Buffer event = Buffer.buffer(transcoded.length() + 8)
          .appendString("data: ")
          .appendBuffer(transcoded)
          .appendString("\n\n");
        res = httpResponse.write(event);
      } else {
        httpResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(transcoded.length()));
        res = httpResponse.write(transcoded);
      }
    } catch (Exception e) {
      httpResponse.setStatusCode(500).end();
      res = context.failedFuture(e);
    }
    if (head != null) {
      res.onComplete(head);
      head = null;
    }
    return res;
  }
}
