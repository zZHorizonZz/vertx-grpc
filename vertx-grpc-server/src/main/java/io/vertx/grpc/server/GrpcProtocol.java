package io.vertx.grpc.server;

import io.vertx.core.http.HttpVersion;

import java.util.EnumSet;

/**
 * Describe the underlying gRPC protocol, i.e. how gRPC messages are framed over HTTP.
 * <p>
 * A protocol does not carry a single media type: the HTTP content-type is a function of the framing and the message
 * {@link io.vertx.grpc.common.WireFormat}, and for transcoding it is determined dynamically by the registered
 * {@code GrpcHttpInvoker}s. Content-type handling therefore lives with request classification and the response streams,
 * not on this enum.
 */
public enum GrpcProtocol {

  /**
   * gRPC over HTTP/2
   */
  HTTP_2(EnumSet.of(HttpVersion.HTTP_2)),

  /**
   * gRPC transcoding HTTP/1
   */
  TRANSCODING(EnumSet.allOf(HttpVersion.class)),

  /**
   * gRPC Web
   */
  WEB(EnumSet.allOf(HttpVersion.class)),

  /**
   * gRPC Web text
   */
  WEB_TEXT(EnumSet.allOf(HttpVersion.class));

  private final EnumSet<HttpVersion> acceptedVersions;

  GrpcProtocol(EnumSet<HttpVersion> acceptedVersions) {
    this.acceptedVersions = acceptedVersions;
  }

  /**
   * @return whether the protocol accepts the HTTP {@code version}
   */
  public boolean accepts(HttpVersion version) {
    return acceptedVersions.contains(version);
  }
}
