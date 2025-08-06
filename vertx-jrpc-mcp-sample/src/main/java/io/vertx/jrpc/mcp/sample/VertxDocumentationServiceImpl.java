package io.vertx.jrpc.mcp.sample;

import com.google.protobuf.Descriptors;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.sample.vertxdocs.*;
import io.vertx.mcp.proto.ResourceContent;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Service that fetches Vert.x documentation index and exposes: - ReadDocumentation: returns ResourceContent with TextResourceContent - SearchDocumentation: returns a custom
 * SearchDocumentationResponse
 */
public class VertxDocumentationServiceImpl implements Service {

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  public static final ServiceName SERVICE_NAME = ServiceName.create(VertxDocumentationServiceGrpc.SERVICE_NAME);

  public static final GrpcMessageDecoder<ReadDocumentationRequest> READ_REQ_DEC = GrpcMessageDecoder.decoder(ReadDocumentationRequest.newBuilder());
  public static final GrpcMessageEncoder<ResourceContent> READ_RES_ENC = GrpcMessageEncoder.encoder();

  public static final GrpcMessageDecoder<SearchDocumentationRequest> SEARCH_REQ_DEC = GrpcMessageDecoder.decoder(SearchDocumentationRequest.newBuilder());
  public static final GrpcMessageEncoder<SearchDocumentationResponse> SEARCH_RES_ENC = GrpcMessageEncoder.encoder();

  @Override
  public ServiceName name() {
    return SERVICE_NAME;
  }

  @Override
  public Descriptors.ServiceDescriptor descriptor() {
    return VertxDocumentationServiceOuterClass.getDescriptor().findServiceByName("VertxDocumentationService");
  }

  @Override
  public void bind(GrpcServer server) {
    server.callHandler(new ReadDocumentationMethod(), request -> request.handler(body -> {
      String slug = new String(Base64.getDecoder().decode(body.getSlug()), StandardCharsets.UTF_8);
      readDoc(body.getVersion(), slug).whenComplete((rc, err) -> {
        ResourceContent res = rc != null ? rc : ResourceContent.newBuilder()
          .setUri("")
          .setText("Not found")
          .setMimeType("text/plain")
          .build();
        request.response().end(res);
      });
    }));

    server.callHandler(new SearchDocumentationMethod(), request -> request.handler(body -> {
      searchDocs(body.getVersion(), body.getQuery(), 25).whenComplete((resp, err) -> {
        SearchDocumentationResponse safe = resp != null ? resp : SearchDocumentationResponse.newBuilder()
          .setVersion(body.getVersion())
          .setQuery(body.getQuery())
          .build();
        request.response().end(safe);
      });
    }));
  }

  private static class ReadDocumentationMethod implements ServiceMethod<ReadDocumentationRequest, ResourceContent> {
    @Override
    public ServiceName serviceName() {
      return SERVICE_NAME;
    }

    @Override
    public String methodName() {
      return "ReadDocumentation";
    }

    @Override
    public GrpcMessageEncoder<ResourceContent> encoder() {
      return READ_RES_ENC;
    }

    @Override
    public GrpcMessageDecoder<ReadDocumentationRequest> decoder() {
      return READ_REQ_DEC;
    }
  }

  private static class SearchDocumentationMethod implements ServiceMethod<SearchDocumentationRequest, SearchDocumentationResponse> {
    @Override
    public ServiceName serviceName() {
      return SERVICE_NAME;
    }

    @Override
    public String methodName() {
      return "SearchDocumentation";
    }

    @Override
    public GrpcMessageEncoder<SearchDocumentationResponse> encoder() {
      return SEARCH_RES_ENC;
    }

    @Override
    public GrpcMessageDecoder<SearchDocumentationRequest> decoder() {
      return SEARCH_REQ_DEC;
    }
  }

  // Logic
  private CompletableFuture<ResourceContent> readDoc(String version, String slug) {
    if (version == null || version.isEmpty() || slug == null || slug.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    return fetchIndex(version).thenApply(index -> {
      for (int i = 0; i < index.size(); i++) {
        JsonObject obj = index.getJsonObject(i);
        if (slug.equals(obj.getString("slug"))) {
          String body = obj.getString("body", "");
          String uri = buildDocUri(version, slug);
          return ResourceContent.newBuilder()
            .setUri(uri)
            .setText(body)
            .setMimeType("text/plain")
            .build();
        }
      }
      return null;
    }).exceptionally(err -> null);
  }

  private CompletableFuture<SearchDocumentationResponse> searchDocs(String version, String query, int limit) {
    if (version == null || version.isEmpty() || query == null || query.isEmpty()) {
      return CompletableFuture.completedFuture(SearchDocumentationResponse.newBuilder()
        .setVersion(version == null ? "" : version)
        .setQuery(query == null ? "" : query)
        .build());
    }
    int max = limit > 0 ? Math.min(limit, 50) : 10;
    String qLower = query.toLowerCase(Locale.ROOT);
    return fetchIndex(version).thenApply(index -> {
      List<SearchResult> results = new ArrayList<>();
      for (int i = 0; i < index.size() && results.size() < max; i++) {
        JsonObject obj = index.getJsonObject(i);
        String slug = obj.getString("slug", "");
        String body = obj.getString("body", "");
        String hay = (slug + "\n" + body).toLowerCase(Locale.ROOT);
        if (hay.contains(qLower)) {
          String snippet = makeSnippet(body, qLower, 160);
          results.add(SearchResult.newBuilder().setSlug(Base64.getEncoder().encodeToString(slug.getBytes(StandardCharsets.UTF_8))).setSnippet(snippet).build());
        }
      }
      return SearchDocumentationResponse.newBuilder()
        .addAllResults(results)
        .setVersion(version)
        .setQuery(query)
        .build();
    }).exceptionally(err -> SearchDocumentationResponse.newBuilder().setVersion(version).setQuery(query).build());
  }

  private CompletableFuture<JsonArray> fetchIndex(String version) {
    try {
      String safeVersion = URLEncoder.encode(version, StandardCharsets.UTF_8);
      String url = "https://vertx.io/docs/" + safeVersion + "/index.json";
      HttpRequest req = HttpRequest.newBuilder(URI.create(url))
        .header("Accept", "application/json")
        .GET()
        .build();
      return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenApply(body -> new JsonArray(body));
    } catch (Exception e) {
      return CompletableFuture.completedFuture(new JsonArray());
    }
  }

  private static String buildDocUri(String version, String slug) {
    String base = "https://vertx.io/docs/" + version + "/";
    return base + slug;
  }

  private static String makeSnippet(String body, String qLower, int maxLen) {
    if (body == null || body.isEmpty())
      return "";
    String lower = body.toLowerCase(Locale.ROOT);
    int idx = lower.indexOf(qLower);
    int start = Math.max(0, idx >= 0 ? idx - maxLen / 4 : 0);
    int end = Math.min(body.length(), start + maxLen);
    String slice = body.substring(start, end).replaceAll("\n", " ").trim();
    if (start > 0)
      slice = "…" + slice;
    if (end < body.length())
      slice = slice + "…";
    return slice;
  }
}
