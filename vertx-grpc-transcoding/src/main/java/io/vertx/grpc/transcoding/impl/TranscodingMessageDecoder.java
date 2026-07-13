package io.vertx.grpc.transcoding.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.transcoding.impl.config.HttpVariableBinding;

import java.util.List;

public class TranscodingMessageDecoder<Req> implements GrpcMessageDecoder<Req> {

  private final GrpcMessageDecoder<Req> messageDecoder;
  private final WireFormat format;
  private final String transcodingRequestBody;
  private final List<HttpVariableBinding> bindings;
  private final TranscodingBodyFormat bodyFormat;

  public TranscodingMessageDecoder(GrpcMessageDecoder<Req> messageDecoder, WireFormat format, String transcodingRequestBody, List<HttpVariableBinding> bindings, TranscodingBodyFormat bodyFormat) {
    this.messageDecoder = messageDecoder;
    this.format = format;
    this.transcodingRequestBody = transcodingRequestBody;
    this.bindings = bindings;
    this.bodyFormat = bodyFormat;
  }

  @Override
  public Req decode(GrpcMessage msg) throws CodecException {
    Buffer transcoded;
    try {
      transcoded = bodyFormat.transcode(msg.payload(), bindings, transcodingRequestBody, messageDecoder.messageDescriptor());
    } catch (DecodeException e) {
      throw new CodecException(e);
    }
    return messageDecoder.decode(GrpcMessage.message("identity", format, transcoded));
  }

  @Override
  public boolean accepts(WireFormat format) {
    return messageDecoder.accepts(format);
  }
}
