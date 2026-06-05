package io.vertx.grpc.server;

import io.vertx.core.http.HttpVersion;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Describe the underlying gRPC protocol.
 */
public enum GrpcProtocol {

  /**
   * gRPC over HTTP/2
   */
  HTTP_2("application/grpc", EnumSet.of(HttpVersion.HTTP_2)),

  /**
   * gRPC transcoding HTTP/1
   */
  TRANSCODING("application/json", EnumSet.allOf(HttpVersion.class)),

  /**
   * gRPC Web
   */
  WEB("application/grpc-web", EnumSet.allOf(HttpVersion.class)),

  /**
   * gRPC Web text
   */
  WEB_TEXT("application/grpc-web-text", EnumSet.allOf(HttpVersion.class));

  private final String mediaType;
  private final EnumSet<HttpVersion> acceptedVersions;
  private final List<String> mediaTypes;

  GrpcProtocol(String mediaType, EnumSet<HttpVersion> acceptedVersions) {
    this.mediaType = mediaType;
    this.acceptedVersions = acceptedVersions;
    // The gRPC family negotiates the message format with a +proto / +json suffix, transcoding does not
    if (mediaType.startsWith("application/grpc")) {
      this.mediaTypes = Collections.unmodifiableList(Arrays.asList(mediaType, mediaType + "+proto", mediaType + "+json"));
    } else {
      this.mediaTypes = Collections.singletonList(mediaType);
    }
  }

  /**
   * @return whether the protocol accepts the HTTP {@code version}
   */
  public boolean accepts(HttpVersion version) {
    return acceptedVersions.contains(version);
  }

  /**
   * @return the HTTP media type
   */
  public String mediaType() {
    return mediaType;
  }

  /**
   * @return the media types this protocol accepts, including the {@code +proto} and {@code +json} message format
   *         variants for the gRPC family, used to advertise accepted content types in an {@code OPTIONS} response
   */
  public List<String> mediaTypes() {
    return mediaTypes;
  }
}
