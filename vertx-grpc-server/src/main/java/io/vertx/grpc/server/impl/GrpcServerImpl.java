/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.server.impl;

import io.vertx.core.Closeable;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.grpc.common.*;
import io.vertx.grpc.common.impl.GrpcMessageDeframer;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.common.impl.Http2GrpcMessageDeframer;
import io.vertx.grpc.server.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcServerImpl implements GrpcServer, Closeable {

  private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("application/grpc(-web(-text)?)?(\\+(json|proto))?");

  // CORS / preflight header names, defined here to avoid depending on optional HttpHeaders constants
  private static final CharSequence ACCEPT_POST = HttpHeaders.createOptimized("Accept-Post");
  private static final CharSequence ACCESS_CONTROL_ALLOW_ORIGIN = HttpHeaders.createOptimized("Access-Control-Allow-Origin");
  private static final CharSequence ACCESS_CONTROL_ALLOW_METHODS = HttpHeaders.createOptimized("Access-Control-Allow-Methods");
  private static final CharSequence ACCESS_CONTROL_ALLOW_HEADERS = HttpHeaders.createOptimized("Access-Control-Allow-Headers");
  private static final CharSequence ACCESS_CONTROL_EXPOSE_HEADERS = HttpHeaders.createOptimized("Access-Control-Expose-Headers");
  private static final CharSequence ACCESS_CONTROL_ALLOW_CREDENTIALS = HttpHeaders.createOptimized("Access-Control-Allow-Credentials");
  private static final CharSequence ACCESS_CONTROL_MAX_AGE = HttpHeaders.createOptimized("Access-Control-Max-Age");
  private static final CharSequence ACCESS_CONTROL_REQUEST_HEADERS = HttpHeaders.createOptimized("Access-Control-Request-Headers");

  private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

  private final GrpcServerOptions options;
  private final CorsOriginMatcher corsMatcher;
  private Handler<GrpcServerRequest<Buffer, Buffer>> requestHandler;

  private final List<Service> services = new ArrayList<>();
  private final Map<String, List<MethodCallHandler<?, ?>>> methodCallHandlers = new HashMap<>();

  private final List<GrpcHttpInvoker> invokers;

  private boolean closing;

  public GrpcServerImpl(Vertx vertx, GrpcServerOptions options) {
    ServiceLoader<GrpcHttpInvoker> loader = ServiceLoader.load(GrpcHttpInvoker.class);
    this.invokers = loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
    this.options = new GrpcServerOptions(Objects.requireNonNull(options, "options is null"));
    this.corsMatcher = this.options.getCors() != null ? new CorsOriginMatcher(this.options.getCors()) : null;
  }

  @Override
  public void close(Completable<Void> completion) {
    List<Service> toClose;
    synchronized (this) {
      closing = true;
      toClose = new ArrayList<>(services);
      services.clear();
    }
    List<Future<Void>> futures = toClose
      .stream()
      .map(Service::close)
      .collect(Collectors.toList());
    Future
      .all(futures)
      .<Void>mapEmpty()
      .onComplete(completion);
  }

  @Override
  public void handle(HttpServerRequest httpRequest) {
    if (httpRequest.method() == HttpMethod.OPTIONS) {
      handlePreflight(httpRequest);
      return;
    }

    GrpcServerRequestInspector.RequestInspectionDetails details = GrpcServerRequestInspector.inspect(httpRequest);
    if (details != null) {
      int errorCode = validate(details);
      if (errorCode > 0) {
        httpRequest.response().setStatusCode(errorCode).end();
        return;
      }
    } else {
      log.trace("invalid content-type header " + httpRequest.getHeader(HttpHeaders.CONTENT_TYPE) + ", sending error 415");
      httpRequest.response().setStatusCode(415).end();
      return;
    }

    GrpcMethodCall methodCall = new GrpcMethodCall(httpRequest.path());
    String path = httpRequest.path();
    while (true) {
      List<MethodCallHandler<?, ?>> mchList = methodCallHandlers.get(path);
      if (mchList != null) {
        for (MethodCallHandler<?, ?> mch : mchList) {
          if (handle(mch, httpRequest, methodCall, details.protocol, details.format)) {
            return;
          }
        }
      }
      int idx = path.lastIndexOf('/');
      if (idx <= 0) {
        break;
      }
      path = path.substring(0, idx);
    }

    // Generic handling
    Handler<GrpcServerRequest<Buffer, Buffer>> handler = requestHandler;
    if (handler != null) {
      handle(new MethodCallHandler<>(null, GrpcMessageDecoder.IDENTITY, GrpcMessageEncoder.IDENTITY, handler), httpRequest, methodCall, details.protocol, details.format);
    } else {
      String msg = "Method not found: " + httpRequest.path().substring(1);
      HttpServerResponse response = httpRequest.response();
      boolean webText = true;
      switch (details.protocol) {
        case HTTP_2:
        case WEB:
        case WEB_TEXT:
          response.setStatusCode(200);
          response.putHeader(HttpHeaders.CONTENT_TYPE, details.protocol.mediaType());
          response.putHeader(GrpcHeaderNames.GRPC_STATUS, GrpcStatus.UNIMPLEMENTED.toString());
          response.putHeader(GrpcHeaderNames.GRPC_MESSAGE, msg);
          response.end();
          break;
        default:
          response
            .setStatusCode(500)
            .end();
          break;
      }
    }
  }

  private int validate(GrpcServerRequestInspector.RequestInspectionDetails details) {
    // Check HTTP version compatibility
    if (!details.protocol.accepts(details.version)) {
      log.trace(details.protocol.mediaType() + " not supported on " + details.version + ", sending error 415");
      return 415;
    }

    // Check config
    if (!options.isProtocolEnabled(details.protocol)) {
      log.trace(details.protocol + " is not supported, sending error 415");
      return 415;
    }

    return -1;
  }

  /**
   * Answer an {@code OPTIONS} request by advertising the HTTP methods ({@code Allow}) and content types
   * ({@code Accept-Post}) accepted at the request path, plus the CORS headers when configured. The server is the only
   * writer: the built-in protocols and every invoker only contribute information.
   */
  private void handlePreflight(HttpServerRequest httpRequest) {
    String requestPath = httpRequest.path();

    List<GrpcProtocol> grpcProtocols = new ArrayList<>();
    for (GrpcProtocol protocol : EnumSet.of(GrpcProtocol.HTTP_2, GrpcProtocol.WEB, GrpcProtocol.WEB_TEXT)) {
      if (options.isProtocolEnabled(protocol)) {
        grpcProtocols.add(protocol);
      }
    }
    boolean transcodingEnabled = options.isProtocolEnabled(GrpcProtocol.TRANSCODING);

    Set<HttpMethod> allowedMethods = new LinkedHashSet<>();
    Set<String> acceptPost = new LinkedHashSet<>();
    boolean matched = false;

    Set<MethodCallHandler<?, ?>> visited = new HashSet<>();
    String path = requestPath;
    while (true) {
      List<MethodCallHandler<?, ?>> mchList = methodCallHandlers.get(path);
      if (mchList != null) {
        for (MethodCallHandler<?, ?> mch : mchList) {
          if (mch.method == null || !visited.add(mch)) {
            continue;
          }
          // The built-in gRPC protocols only bind at the exact full method path
          if (requestPath.equals("/" + mch.method.fullMethodName()) && !grpcProtocols.isEmpty()) {
            matched = true;
            allowedMethods.add(HttpMethod.POST);
            for (GrpcProtocol protocol : grpcProtocols) {
              acceptPost.addAll(protocol.mediaTypes());
            }
          }
          // Transcoding binds via path templates, the invoker reports the verbs for this path
          if (transcodingEnabled) {
            for (GrpcHttpInvoker invoker : invokers) {
              PreflightInfo info = invoker.preflight(httpRequest, mch.method);
              if (!info.methods().isEmpty()) {
                matched = true;
                allowedMethods.addAll(info.methods());
                acceptPost.addAll(info.acceptPostMediaTypes());
              }
            }
          }
        }
      }
      int idx = path.lastIndexOf('/');
      if (idx <= 0) {
        break;
      }
      path = path.substring(0, idx);
    }

    // A generic call handler accepts any path over the gRPC protocols
    if (!matched && requestHandler != null && !grpcProtocols.isEmpty()) {
      matched = true;
      allowedMethods.add(HttpMethod.POST);
      for (GrpcProtocol protocol : grpcProtocols) {
        acceptPost.addAll(protocol.mediaTypes());
      }
    }

    HttpServerResponse response = httpRequest.response();
    if (!matched) {
      response.setStatusCode(404).end();
      return;
    }

    allowedMethods.add(HttpMethod.OPTIONS);
    response.putHeader(HttpHeaders.ALLOW, methodsToString(allowedMethods));
    if (!acceptPost.isEmpty()) {
      response.putHeader(ACCEPT_POST, String.join(", ", acceptPost));
    }

    applyCors(httpRequest, response, allowedMethods);

    response.setStatusCode(204).end();
  }

  private void applyCors(HttpServerRequest request, HttpServerResponse response, Set<HttpMethod> allowedMethods) {
    GrpcCorsOptions cors = options.getCors();
    if (cors == null || corsMatcher == null) {
      return;
    }
    String origin = request.getHeader(HttpHeaders.ORIGIN);
    if (origin == null || !corsMatcher.isAllowed(origin)) {
      return;
    }
    boolean credentials = cors.getAllowCredentials();
    if (corsMatcher.allowAll() && !credentials) {
      response.putHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    } else {
      response.putHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
      response.putHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    }
    if (credentials) {
      response.putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
    response.putHeader(ACCESS_CONTROL_ALLOW_METHODS, methodsToString(allowedMethods));
    Set<String> allowedHeaders = cors.getAllowedHeaders();
    if (allowedHeaders != null && !allowedHeaders.isEmpty()) {
      response.putHeader(ACCESS_CONTROL_ALLOW_HEADERS, String.join(", ", allowedHeaders));
    } else {
      String requested = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
      if (requested != null) {
        response.putHeader(ACCESS_CONTROL_ALLOW_HEADERS, requested);
      }
    }
    Set<String> exposedHeaders = cors.getExposedHeaders();
    if (exposedHeaders != null && !exposedHeaders.isEmpty()) {
      response.putHeader(ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", exposedHeaders));
    }
    int maxAge = cors.getMaxAgeSeconds();
    if (maxAge >= 0) {
      response.putHeader(ACCESS_CONTROL_MAX_AGE, Integer.toString(maxAge));
    }
  }

  private static String methodsToString(Set<HttpMethod> methods) {
    StringBuilder sb = new StringBuilder();
    for (HttpMethod method : methods) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(method.name());
    }
    return sb.toString();
  }

  private <Req, Resp> boolean handle(MethodCallHandler<Req, Resp> method, HttpServerRequest httpRequest, GrpcMethodCall methodCall, GrpcProtocol protocol, WireFormat format) {
    io.vertx.core.internal.ContextInternal context = ((HttpServerRequestInternal) httpRequest).context();

    String encoding = httpRequest.headers().get(GrpcHeaderNames.GRPC_ENCODING);

    HttpGrpcOutboundStream outboundInvoker;
    GrpcMessageDecoder<Req> messageDecoder;
    switch (protocol) {
      case HTTP_2:
        if (method.method != null && !httpRequest.path().equals("/" + method.method.fullMethodName())) {
          return false;
        }
        outboundInvoker = new Http2GrpcOutboundStream(httpRequest, new Http2GrpcMessageDeframer(encoding, format));
        messageDecoder = method.messageDecoder;
        break;
      case WEB:
      case WEB_TEXT:
        if (method.method != null && !httpRequest.path().equals("/" + method.method.fullMethodName())) {
          return false;
        }
        GrpcMessageDeframer deframer;
        if (httpRequest.version() != HttpVersion.HTTP_2 && GrpcMediaType.isGrpcWebText(httpRequest.getHeader(CONTENT_TYPE))) {
          deframer  = new TextMessageDeframer();
        } else {
          deframer  = new Http2GrpcMessageDeframer(encoding, format);
        }
        outboundInvoker = new WebGrpcOutboundStream(httpRequest, protocol, deframer);
        messageDecoder = method.messageDecoder;
        break;
      case TRANSCODING:
        GrpcInvocation invocation = null;
        for (GrpcHttpInvoker invoker : invokers) {
          invocation = invoker.accept(httpRequest, method.method);
          if (invocation != null) {
            break;
          }
        }
        if (invocation != null) {
          outboundInvoker = invocation.outboundInvoker;
          messageDecoder = (GrpcMessageDecoder)invocation.messageDecoder;
          break;
        } else {
          return false;
        }
      default:
        throw new AssertionError();
    }

    outboundInvoker.init();

    GrpcDispatcher<Req, Resp> dispatcher = new GrpcDispatcher<>(
      outboundInvoker,
      context,
      protocol,
      format,
      messageDecoder,
      methodCall,
      httpRequest.connection(),
      method,
      options.getDeadlinePropagation(),
      options.getScheduleDeadlineAutomatically());
    outboundInvoker.handler(dispatcher);
    outboundInvoker.exceptionHandler(dispatcher::handleException);
    outboundInvoker.endHandler(v -> dispatcher.handleEnd());

    outboundInvoker.init(httpRequest, options.getMaxMessageSize());

    return true;
  }

  public synchronized GrpcServer callHandler(Handler<GrpcServerRequest<Buffer, Buffer>> handler) {
    if (closing) {
      throw new IllegalStateException("Server closed");
    }
    this.requestHandler = handler;
    return this;
  }

  private <Req, Resp> void registerMethodCallHandler(String path, MethodCallHandler<Req, Resp> mch) {
    methodCallHandlers.computeIfAbsent(path, k -> new ArrayList<>()).add(mch);
  }

  private <Req, Resp> void unregisterMethodCallHandler(String path, ServiceMethod<Req, Resp> serviceMethod) {
    methodCallHandlers.computeIfPresent(path, (p, registrations) -> {
      registrations.removeIf(mch -> mch.method.equals(serviceMethod));
      return registrations.isEmpty() ? null : registrations;
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized <Req, Resp> GrpcServer callHandler(ServiceMethod<Req, Resp> serviceMethod, Handler<GrpcServerRequest<Req, Resp>> handler) {
    if (closing) {
      throw new IllegalStateException("Server closed");
    }
    if (handler != null) {
      MethodCallHandler<Req, Resp> p = new MethodCallHandler<>(serviceMethod, serviceMethod.decoder(), serviceMethod.encoder(), handler);
      if (serviceMethod instanceof MountPoint) {
        MountPoint<Req, Resp> mountPoint = (MountPoint<Req, Resp>) serviceMethod;
        List<String> paths = mountPoint.paths();
        for (String path : paths) {
          registerMethodCallHandler(path, p);
        }
      }
      registerMethodCallHandler("/" + serviceMethod.fullMethodName(), p);
    } else {
      if (serviceMethod instanceof MountPoint) {
        MountPoint<Req, Resp> mountPoint = (MountPoint<Req, Resp>) serviceMethod;
        List<String> paths = mountPoint.paths();
        for (String path : paths) {
          unregisterMethodCallHandler(path, serviceMethod);
        }
      }
      unregisterMethodCallHandler("/" + serviceMethod.fullMethodName(), serviceMethod);
    }
    return this;
  }

  @Override
  public GrpcServer addService(Service service) {
    synchronized (this) {
      if (closing) {
        throw new IllegalStateException("Server closed");
      }
      for (Service s : this.services) {
        if (s.name().equals(service.name())) {
          throw new IllegalStateException("Duplicated name: " + service.name().name());
        }
      }
      if (service instanceof ServerAware) {
        ((ServerAware)service).setServer(this);
      }
      for (ServiceMethod method : service.methods()) {
        ServiceMethodInvoker invoker = service.invoker(method);
        registerMethodCallHandler(service.pathOfMethod(method.methodName()), new MethodCallHandler<Object, Object>(method, method.decoder(), method.encoder(), invoker));
      }

      this.services.add(service);
    }

    return this;
  }

  @Override
  public List<Service> services() {
    return Collections.unmodifiableList(services);
  }

  static class MethodCallHandler<Req, Resp> implements Handler<GrpcServerRequest<Req, Resp>> {

    final ServiceMethod<Req, Resp> method;
    final GrpcMessageDecoder<Req> messageDecoder;
    final GrpcMessageEncoder<Resp> messageEncoder;
    final ServiceMethodInvoker<Req, Resp> invoker;

    MethodCallHandler(ServiceMethod<Req, Resp> method, GrpcMessageDecoder<Req> messageDecoder, GrpcMessageEncoder<Resp> messageEncoder, Handler<GrpcServerRequest<Req, Resp>> handler) {
      this.method = method;
      this.messageDecoder = messageDecoder;
      this.messageEncoder = messageEncoder;
      this.invoker = handler::handle;
    }

    MethodCallHandler(ServiceMethod<Req, Resp> method, GrpcMessageDecoder<Req> messageDecoder, GrpcMessageEncoder<Resp> messageEncoder, ServiceMethodInvoker<Req, Resp> invoker) {
      this.method = method;
      this.messageDecoder = messageDecoder;
      this.messageEncoder = messageEncoder;
      this.invoker = invoker;
    }

    @Override
    public void handle(GrpcServerRequest<Req, Resp> grpcRequest) {
      try {
        invoker.invoke(grpcRequest);
      } catch (Exception e) {
        grpcRequest.response().fail(e);
      }
    }
  }
}
