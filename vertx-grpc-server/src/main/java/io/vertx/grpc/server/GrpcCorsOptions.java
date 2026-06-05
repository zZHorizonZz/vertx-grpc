/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.server;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Unstable;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcHeaderNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Cross-Origin Resource Sharing configuration for the {@link GrpcServer} preflight ({@code OPTIONS}) handling.
 * <p>
 * When a {@link GrpcCorsOptions} instance is set on {@link GrpcServerOptions}, the server answers browser preflight
 * requests itself with the appropriate {@code Access-Control-*} headers. This is self-contained and does not require a
 * fronting CORS handler, which would otherwise intercept the preflight before it reaches the gRPC server.
 */
@DataObject
@JsonGen(publicConverter = false)
@Unstable
public class GrpcCorsOptions {

  /**
   * The default request headers allowed by a preflight response, covering the gRPC-Web client headers.
   */
  public static final Set<String> DEFAULT_ALLOWED_HEADERS = new LinkedHashSet<>(Arrays.asList(
    HttpHeaders.CONTENT_TYPE.toString(),
    "x-grpc-web",
    "x-user-agent",
    GrpcHeaderNames.GRPC_TIMEOUT.toString(),
    GrpcHeaderNames.GRPC_ENCODING.toString(),
    GrpcHeaderNames.GRPC_ACCEPT_ENCODING.toString()));

  /**
   * The default response headers exposed by a preflight response, covering the gRPC trailers a browser client must read.
   */
  public static final Set<String> DEFAULT_EXPOSED_HEADERS = new LinkedHashSet<>(Arrays.asList(
    GrpcHeaderNames.GRPC_STATUS.toString(),
    GrpcHeaderNames.GRPC_MESSAGE.toString(),
    GrpcHeaderNames.GRPC_STATUS_DETAILS_BIN.toString()));

  /**
   * Whether credentials are allowed by default = {@code false}.
   */
  public static final boolean DEFAULT_ALLOW_CREDENTIALS = false;

  /**
   * The default max age in seconds = {@code -1} (the {@code Access-Control-Max-Age} header is omitted).
   */
  public static final int DEFAULT_MAX_AGE_SECONDS = -1;

  private List<String> allowedOrigins;
  private List<String> allowedOriginPatterns;
  private boolean allowCredentials;
  private Set<String> allowedHeaders;
  private Set<String> exposedHeaders;
  private int maxAgeSeconds;

  /**
   * Default options.
   */
  public GrpcCorsOptions() {
    allowedOrigins = new ArrayList<>();
    allowedOriginPatterns = new ArrayList<>();
    allowCredentials = DEFAULT_ALLOW_CREDENTIALS;
    allowedHeaders = new LinkedHashSet<>(DEFAULT_ALLOWED_HEADERS);
    exposedHeaders = new LinkedHashSet<>(DEFAULT_EXPOSED_HEADERS);
    maxAgeSeconds = DEFAULT_MAX_AGE_SECONDS;
  }

  /**
   * Copy constructor.
   */
  public GrpcCorsOptions(GrpcCorsOptions other) {
    allowedOrigins = new ArrayList<>(other.allowedOrigins);
    allowedOriginPatterns = new ArrayList<>(other.allowedOriginPatterns);
    allowCredentials = other.allowCredentials;
    allowedHeaders = new LinkedHashSet<>(other.allowedHeaders);
    exposedHeaders = new LinkedHashSet<>(other.exposedHeaders);
    maxAgeSeconds = other.maxAgeSeconds;
  }

  /**
   * Creates options from JSON.
   */
  public GrpcCorsOptions(JsonObject json) {
    this();
    GrpcCorsOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the exact origins allowed to perform cross-origin requests
   */
  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  /**
   * Set the exact origins allowed to perform cross-origin requests. A single entry of {@code *} allows any origin,
   * which is only honored when credentials are not allowed.
   *
   * @param allowedOrigins the allowed origins
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
    return this;
  }

  /**
   * Add an exact origin allowed to perform cross-origin requests.
   *
   * @param origin the allowed origin
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions addAllowedOrigin(String origin) {
    allowedOrigins.add(origin);
    return this;
  }

  /**
   * Remove an exact allowed origin.
   *
   * @param origin the origin to remove
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions removeAllowedOrigin(String origin) {
    allowedOrigins.remove(origin);
    return this;
  }

  /**
   * @return the origin patterns (regular expressions) allowed to perform cross-origin requests
   */
  public List<String> getAllowedOriginPatterns() {
    return allowedOriginPatterns;
  }

  /**
   * Set the origin patterns (regular expressions) allowed to perform cross-origin requests.
   *
   * @param allowedOriginPatterns the allowed origin patterns
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    this.allowedOriginPatterns = allowedOriginPatterns;
    return this;
  }

  /**
   * Add an origin pattern (regular expression) allowed to perform cross-origin requests.
   *
   * @param pattern the allowed origin pattern
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions addAllowedOriginPattern(String pattern) {
    allowedOriginPatterns.add(pattern);
    return this;
  }

  /**
   * Remove an allowed origin pattern (regular expression).
   *
   * @param pattern the pattern to remove
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions removeAllowedOriginPattern(String pattern) {
    allowedOriginPatterns.remove(pattern);
    return this;
  }

  /**
   * @return whether credentials are allowed, mapping to the {@code Access-Control-Allow-Credentials} header
   */
  public boolean getAllowCredentials() {
    return allowCredentials;
  }

  /**
   * Set whether credentials are allowed. When allowed, the server echoes the request origin rather than {@code *}.
   *
   * @param allowCredentials whether credentials are allowed
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions setAllowCredentials(boolean allowCredentials) {
    this.allowCredentials = allowCredentials;
    return this;
  }

  /**
   * @return the request headers allowed by a preflight response
   */
  public Set<String> getAllowedHeaders() {
    return allowedHeaders;
  }

  /**
   * Set the request headers allowed by a preflight response, mapping to the {@code Access-Control-Allow-Headers} header.
   *
   * @param allowedHeaders the allowed request headers
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions setAllowedHeaders(Set<String> allowedHeaders) {
    this.allowedHeaders = allowedHeaders;
    return this;
  }

  /**
   * Add a request header allowed by a preflight response.
   *
   * @param header the allowed request header
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions addAllowedHeader(String header) {
    allowedHeaders.add(header);
    return this;
  }

  /**
   * Remove a request header from the set allowed by a preflight response, including one of the defaults.
   *
   * @param header the header to remove
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions removeAllowedHeader(String header) {
    allowedHeaders.remove(header);
    return this;
  }

  /**
   * @return the response headers exposed to the client
   */
  public Set<String> getExposedHeaders() {
    return exposedHeaders;
  }

  /**
   * Set the response headers exposed to the client, mapping to the {@code Access-Control-Expose-Headers} header.
   *
   * @param exposedHeaders the exposed response headers
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions setExposedHeaders(Set<String> exposedHeaders) {
    this.exposedHeaders = exposedHeaders;
    return this;
  }

  /**
   * Add a response header exposed to the client.
   *
   * @param header the exposed response header
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions addExposedHeader(String header) {
    exposedHeaders.add(header);
    return this;
  }

  /**
   * Remove a response header from the set exposed to the client, including one of the defaults.
   *
   * @param header the header to remove
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions removeExposedHeader(String header) {
    exposedHeaders.remove(header);
    return this;
  }

  /**
   * @return the number of seconds a preflight response may be cached, or a negative value to omit the header
   */
  public int getMaxAgeSeconds() {
    return maxAgeSeconds;
  }

  /**
   * Set the number of seconds a preflight response may be cached, mapping to the {@code Access-Control-Max-Age} header.
   * A negative value omits the header.
   *
   * @param maxAgeSeconds the max age in seconds
   * @return a reference to this, so the API can be used fluently
   */
  public GrpcCorsOptions setMaxAgeSeconds(int maxAgeSeconds) {
    this.maxAgeSeconds = maxAgeSeconds;
    return this;
  }

  /**
   * @return a JSON representation of options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    GrpcCorsOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
