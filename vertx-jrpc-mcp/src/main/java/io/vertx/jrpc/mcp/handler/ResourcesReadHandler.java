package io.vertx.jrpc.mcp.handler;

import com.google.protobuf.ByteString;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.grpc.common.*;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.jrpc.mcp.ModelContextProtocolResource;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceProvider;
import io.vertx.jrpc.mcp.ModelContextProtocolResourceTemplate;
import io.vertx.jrpc.mcp.ModelContextProtocolService;
import io.vertx.jrpc.mcp.proto.ResourcesReadRequest;
import io.vertx.jrpc.mcp.proto.ResourcesReadResponse;
import io.vertx.mcp.proto.ResourceContent;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handler for the ResourcesRead RPC method.
 */
public class ResourcesReadHandler extends BaseHandler<ResourcesReadRequest, ResourcesReadResponse> {

  public static final ServiceMethod<ResourcesReadRequest, ResourcesReadResponse> SERVICE_METHOD = ServiceMethod.server(
    ServiceName.create("io.modelcontextprotocol.ModelContextProtocolService"),
    "ResourcesRead",
    GrpcMessageEncoder.encoder(),
    GrpcMessageDecoder.decoder(ResourcesReadRequest.newBuilder()));

  /**
   * Creates a new resources read handler.
   *
   * @param server the gRPC server
   * @param service the MCP service implementation
   */
  public ResourcesReadHandler(GrpcServer server, ModelContextProtocolService service) {
    super(server, service);
  }

  @Override
  public void handle(GrpcServerRequest<ResourcesReadRequest, ResourcesReadResponse> request) {
    request.handler(req -> {
      try {
        service.resourcesList().forEach(p -> handleRequest(request, p, URI.create(req.getUri())));
      } catch (Exception e) {
        request.response().status(GrpcStatus.INTERNAL).end();
      }
    });
  }

  private void handleRequest(GrpcServerRequest<ResourcesReadRequest, ResourcesReadResponse> request, ModelContextProtocolResourceProvider provider, URI uri) {
    provider.apply(uri).onComplete(ar -> {
      if (ar.failed()) {
        request.response().status(GrpcStatus.INTERNAL).end();
        return;
      }

      List<ModelContextProtocolResource> resources = ar.result();
      if (resources == null || resources.isEmpty()) {
        return;
      }

      List<Future<ResourceContent>> builders = new ArrayList<>();
      for (ModelContextProtocolResource res : resources) {
        if (res instanceof ModelContextProtocolResource.TextResource) {
          ModelContextProtocolResource.TextResource tr = (ModelContextProtocolResource.TextResource) res;
          Future<ResourceContent> textFut = tr.text().map(text -> ResourceContent.newBuilder()
            .setUri(safeUriString(res.uri()))
            .setText(text == null ? "" : text)
            .setMimeType(nullToEmpty(res.mimeType()))
            .build());
          builders.add(textFut.map(t -> t));
        } else if (res instanceof ModelContextProtocolResource.BinaryResource) {
          ModelContextProtocolResource.BinaryResource br = (ModelContextProtocolResource.BinaryResource) res;
          Future<ResourceContent> blobFut = br.blob().map(Buffer::getBytes).map(bytes -> Base64.getEncoder().encodeToString(bytes))
            .map(b64 -> ResourceContent.newBuilder()
              .setUri(safeUriString(res.uri()))
              .setBlob(ByteString.copyFrom(b64, StandardCharsets.UTF_8))
              .setMimeType(nullToEmpty(res.mimeType()))
              .build());
          builders.add(blobFut.map(b -> b));
        } else {
          // Fallback: treat as binary by reading generic content()
          Future<ResourceContent> blobFut = res.content().map(Buffer::getBytes).map(bytes -> Base64.getEncoder().encodeToString(bytes))
            .map(b64 -> ResourceContent.newBuilder()
              .setUri(safeUriString(res.uri()))
              .setBlob(ByteString.copyFrom(b64, StandardCharsets.UTF_8))
              .setMimeType(nullToEmpty(res.mimeType()))
              .build());
          builders.add(blobFut.map(b -> b));
        }
      }

      final int total = builders.size();
      java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
      java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean(false);
      List<ResourceContent> contents = new ArrayList<>();

      for (Future<ResourceContent> f : builders) {
        f.onComplete(fr -> {
          if (fr.failed()) {
            if (failed.compareAndSet(false, true)) {
              request.response().status(GrpcStatus.INTERNAL).end();
            }
            return;
          }

          synchronized (contents) {
            contents.add(fr.result());
          }

          if (completed.incrementAndGet() == total && !failed.get()) {
            ResourcesReadResponse response = ResourcesReadResponse.newBuilder()
              .addAllContents(contents)
              .build();
            request.response().end(response);
          }
        });
      }
    });
  }

  private static boolean templateMatchesUri(ModelContextProtocolResourceTemplate template, URI uri) {
    if (template == null)
      return true;
    String tpl = nullToEmpty(template.uriTemplate());
    String path = nullToEmpty(uri.getPath());

    if (tpl.isEmpty())
      return true;

    // Normalize leading slash
    if (!tpl.startsWith("/"))
      tpl = "/" + tpl;

    if (tpl.equals(path))
      return true;

    // Convert template with {var} to regex
    String regex = tpl.replaceAll("\\{[^/]+\\}", "[^/]+");
    Pattern p = Pattern.compile("^" + regex + "$");
    return p.matcher(path).matches() || path.startsWith(tpl);
  }

  private static String safeUriString(URI uri) {
    return uri == null ? "" : uri.toString();
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
