package io.vertx.jrpc.mcp.sample;

import com.google.protobuf.Descriptors;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.Service;
import io.vertx.jrpc.mcp.sample.content.*;
import io.vertx.mcp.proto.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ContentServiceImpl implements Service {

  public static final ServiceName CONTENT_SERVICE_NAME = ServiceName.create(ContentServiceGrpc.SERVICE_NAME);

  // Message encoders and decoders
  public static GrpcMessageDecoder<PostRequest> POST_REQUEST_DECODER = GrpcMessageDecoder.decoder(PostRequest.newBuilder());
  public static GrpcMessageEncoder<TextContent> TEXT_CONTENT_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageEncoder<Content> CONTENT_ENCODER = GrpcMessageEncoder.encoder();
  public static GrpcMessageDecoder<UserRequest> USER_REQUEST_DECODER = GrpcMessageDecoder.decoder(UserRequest.newBuilder());
  public static GrpcMessageDecoder<ResourceRequest> RESOURCE_REQUEST_DECODER = GrpcMessageDecoder.decoder(ResourceRequest.newBuilder());

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  @Override
  public ServiceName name() {
    return CONTENT_SERVICE_NAME;
  }

  @Override
  public Descriptors.ServiceDescriptor descriptor() {
    return ContentServiceOuterClass.getDescriptor().findServiceByName("ContentService");
  }

  @Override
  public void bind(GrpcServer server) {
    // GetPost method - returns TextContent
    server.callHandler(new GetPostMethod(), request -> request.handler(requestBody -> {
      int postId = requestBody.getPostId();
      fetchPost(postId).whenComplete((textContent, err) -> {
        TextContent response = textContent != null ? textContent : TextContent.newBuilder().setText("Error fetching post").build();
        request.response().end(response);
      });
    }));

    // GetRichPost method - returns Content with multiple types
    server.callHandler(new GetRichPostMethod(), request -> request.handler(requestBody -> {
      int postId = requestBody.getPostId();
      fetchRichPost(postId).whenComplete((content, err) -> {
        Content response = content != null ? content : Content.newBuilder().addContent(ContentItem.newBuilder().setText(TextContent.newBuilder().setText("Error fetching rich post").build()).build()).build();
        request.response().end(response);
      });
    }));

    // GetUserProfile method - returns Content with user info and avatar
    server.callHandler(new GetUserProfileMethod(), request -> request.handler(requestBody -> {
      int userId = requestBody.getUserId();
      fetchUserProfile(userId).whenComplete((content, err) -> {
        Content response = content != null ? content :
          Content.newBuilder()
            .addContent(ContentItem.newBuilder()
              .setText(TextContent.newBuilder().setText("Error fetching user profile").build())
              .build())
            .build();
        request.response().end(response);
      });
    }));

    // GetResourceLink method - returns Content with resource links
    server.callHandler(new GetResourceLinkMethod(), request -> request.handler(requestBody -> {
      String resourceType = requestBody.getResourceType();
      String topic = requestBody.getTopic();
      fetchResourceLink(resourceType, topic).whenComplete((content, err) -> {
        Content response = content != null ? content :
          Content.newBuilder()
            .addContent(ContentItem.newBuilder()
              .setText(TextContent.newBuilder().setText("Error fetching resource link").build())
              .build())
            .build();
        request.response().end(response);
      });
    }));
  }

  // Service method implementations
  private static class GetPostMethod implements ServiceMethod<PostRequest, TextContent> {
    @Override
    public ServiceName serviceName() { return CONTENT_SERVICE_NAME; }
    @Override
    public String methodName() { return "GetPost"; }
    @Override
    public GrpcMessageEncoder<TextContent> encoder() { return TEXT_CONTENT_ENCODER; }
    @Override
    public GrpcMessageDecoder<PostRequest> decoder() { return POST_REQUEST_DECODER; }
  }

  private static class GetRichPostMethod implements ServiceMethod<PostRequest, Content> {
    @Override
    public ServiceName serviceName() { return CONTENT_SERVICE_NAME; }
    @Override
    public String methodName() { return "GetRichPost"; }
    @Override
    public GrpcMessageEncoder<Content> encoder() { return CONTENT_ENCODER; }
    @Override
    public GrpcMessageDecoder<PostRequest> decoder() { return POST_REQUEST_DECODER; }
  }

  private static class GetUserProfileMethod implements ServiceMethod<UserRequest, Content> {
    @Override
    public ServiceName serviceName() { return CONTENT_SERVICE_NAME; }
    @Override
    public String methodName() { return "GetUserProfile"; }
    @Override
    public GrpcMessageEncoder<Content> encoder() { return CONTENT_ENCODER; }
    @Override
    public GrpcMessageDecoder<UserRequest> decoder() { return USER_REQUEST_DECODER; }
  }

  private static class GetResourceLinkMethod implements ServiceMethod<ResourceRequest, Content> {
    @Override
    public ServiceName serviceName() { return CONTENT_SERVICE_NAME; }
    @Override
    public String methodName() { return "GetResourceLink"; }
    @Override
    public GrpcMessageEncoder<Content> encoder() { return CONTENT_ENCODER; }
    @Override
    public GrpcMessageDecoder<ResourceRequest> decoder() { return RESOURCE_REQUEST_DECODER; }
  }

  // API integration methods
  private CompletableFuture<TextContent> fetchPost(int postId) {
    String url = "https://jsonplaceholder.typicode.com/posts/" + postId;
    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(body -> {
        try {
          JsonObject json = new JsonObject(body);
          String title = json.getString("title", "No title");
          String bodyText = json.getString("body", "No content");
          String fullText = "Title: " + title + "\n\nContent: " + bodyText;
          return TextContent.newBuilder().setText(fullText).build();
        } catch (Exception e) {
          return TextContent.newBuilder().setText("Error parsing post data").build();
        }
      })
      .exceptionally(err -> TextContent.newBuilder().setText("Error fetching post: " + err.getMessage()).build());
  }

  private CompletableFuture<Content> fetchRichPost(int postId) {
    String url = "https://jsonplaceholder.typicode.com/posts/" + postId;
    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(body -> {
        try {
          JsonObject json = new JsonObject(body);
          String title = json.getString("title", "No title");
          String bodyText = json.getString("body", "No content");

          Content.Builder contentBuilder = Content.newBuilder();

          // Add text content
          contentBuilder.addContent(ContentItem.newBuilder()
            .setText(TextContent.newBuilder()
              .setText("Title: " + title + "\n\nContent: " + bodyText)
              .build())
            .build());

          // Add a sample image (placeholder image)
          String sampleImageData = createSampleImageBase64();
          contentBuilder.addContent(ContentItem.newBuilder()
            .setImage(ImageContent.newBuilder()
              .setData(sampleImageData)
              .setMimeType("image/png")
              .build())
            .build());

          // Add resource link
          contentBuilder.addContent(ContentItem.newBuilder()
            .setResourceLink(ResourceLinkContent.newBuilder()
              .setUri("https://jsonplaceholder.typicode.com/posts/" + postId)
              .setName("Original Post")
              .setDescription("Link to the original JSONPlaceholder post")
              .setMimeType("application/json")
              .build())
            .build());

          return contentBuilder.build();
        } catch (Exception e) {
          return Content.newBuilder()
            .addContent(ContentItem.newBuilder()
              .setText(TextContent.newBuilder().setText("Error parsing rich post data").build())
              .build())
            .build();
        }
      })
      .exceptionally(err -> Content.newBuilder()
        .addContent(ContentItem.newBuilder()
          .setText(TextContent.newBuilder().setText("Error fetching rich post: " + err.getMessage()).build())
          .build())
        .build());
  }

  private CompletableFuture<Content> fetchUserProfile(int userId) {
    String url = "https://jsonplaceholder.typicode.com/users/" + userId;
    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(body -> {
        try {
          JsonObject json = new JsonObject(body);
          String name = json.getString("name", "Unknown User");
          String email = json.getString("email", "No email");
          String website = json.getString("website", "No website");
          String company = json.getJsonObject("company", new JsonObject()).getString("name", "No company");

          Content.Builder contentBuilder = Content.newBuilder();

          // Add user profile text
          String profileText = String.format("Name: %s\nEmail: %s\nWebsite: %s\nCompany: %s",
            name, email, website, company);
          contentBuilder.addContent(ContentItem.newBuilder()
            .setText(TextContent.newBuilder().setText(profileText).build())
            .build());

          // Add avatar image (sample placeholder)
          String avatarData = createAvatarImageBase64();
          contentBuilder.addContent(ContentItem.newBuilder()
            .setImage(ImageContent.newBuilder()
              .setData(avatarData)
              .setMimeType("image/png")
              .build())
            .build());

          return contentBuilder.build();
        } catch (Exception e) {
          return Content.newBuilder()
            .addContent(ContentItem.newBuilder()
              .setText(TextContent.newBuilder().setText("Error parsing user profile data").build())
              .build())
            .build();
        }
      })
      .exceptionally(err -> Content.newBuilder()
        .addContent(ContentItem.newBuilder()
          .setText(TextContent.newBuilder().setText("Error fetching user profile: " + err.getMessage()).build())
          .build())
        .build());
  }

  private CompletableFuture<Content> fetchResourceLink(String resourceType, String topic) {
    return CompletableFuture.supplyAsync(() -> {
      Content.Builder contentBuilder = Content.newBuilder();

      // Add description text
      String description = String.format("Resource links for %s on topic: %s", resourceType, topic);
      contentBuilder.addContent(ContentItem.newBuilder()
        .setText(TextContent.newBuilder().setText(description).build())
        .build());

      // Add different resource links based on type
      switch (resourceType.toLowerCase()) {
        case "documentation":
          contentBuilder.addContent(ContentItem.newBuilder()
            .setResourceLink(ResourceLinkContent.newBuilder()
              .setUri("https://docs.oracle.com/en/java/")
              .setName("Java Documentation")
              .setDescription("Official Java documentation for " + topic)
              .setMimeType("text/html")
              .build())
            .build());
          break;
        case "tutorial":
          contentBuilder.addContent(ContentItem.newBuilder()
            .setResourceLink(ResourceLinkContent.newBuilder()
              .setUri("https://github.com/topics/" + topic.toLowerCase())
              .setName("GitHub Tutorials")
              .setDescription("GitHub repositories and tutorials for " + topic)
              .setMimeType("text/html")
              .build())
            .build());
          break;
        case "api":
          contentBuilder.addContent(ContentItem.newBuilder()
            .setResourceLink(ResourceLinkContent.newBuilder()
              .setUri("https://jsonplaceholder.typicode.com/")
              .setName("JSONPlaceholder API")
              .setDescription("Free fake API for testing and prototyping")
              .setMimeType("application/json")
              .build())
            .build());
          break;
        default:
          contentBuilder.addContent(ContentItem.newBuilder()
            .setResourceLink(ResourceLinkContent.newBuilder()
              .setUri("https://github.com/")
              .setName("GitHub")
              .setDescription("General resource for " + topic)
              .setMimeType("text/html")
              .build())
            .build());
      }

      return contentBuilder.build();
    });
  }

  // Helper methods to create sample images
  private String createSampleImageBase64() {
    // Create a simple 1x1 PNG image (transparent pixel)
    byte[] pngData = {
      (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
      0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
      0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
      0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
      (byte)0x89, 0x00, 0x00, 0x00, 0x0B, 0x49, 0x44, 0x41,
      0x54, 0x78, (byte)0xDA, 0x63, 0x00, 0x01, 0x00, 0x00,
      0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte)0xB4, 0x00,
      0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE,
      0x42, 0x60, (byte)0x82
    };
    return Base64.getEncoder().encodeToString(pngData);
  }

  private String createAvatarImageBase64() {
    // Same simple PNG for avatar
    return createSampleImageBase64();
  }
}
