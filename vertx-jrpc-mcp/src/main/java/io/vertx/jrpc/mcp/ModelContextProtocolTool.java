package io.vertx.jrpc.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jrpc.mcp.proto.Tool;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ModelContextProtocolTool extends Function<JsonObject, Future<ModelContextProtocolTool.ContentDataType>> {
  String id();

  Tool tool();

  ModelContextProtocolService service();

  public interface ContentDataType {
    String type();

    JsonObject toJson();
  }

  public interface StructuredJsonContentDataType extends ContentDataType {
    public static StructuredJsonContentDataType create(JsonObject json) {
      return () -> json;
    }

    @Override
    default String type() {
      return "structured_json";
    }

    @Override
    default JsonObject toJson() {
      return json();
    }

    JsonObject json();
  }

  public interface UnstructuredContentDataType extends ContentDataType {
    public static UnstructuredContentDataType create(List<ContentDataType> content) {
      return () -> content;
    }

    public static UnstructuredContentDataType create(JsonObject json) {
      return () -> {
        JsonArray contentJson = json.getJsonArray("content");
        if (contentJson == null) {
          return List.of();
        }
        return contentJson.stream().map(o -> {
          if (!(o instanceof JsonObject)) {
            throw new IllegalArgumentException("Content must be a json object");
          }

          JsonObject contentJsonObject = (JsonObject) o;
          String type = contentJsonObject.getString("type");
          if (type == null) {
            type = contentJsonObject.getMap().keySet().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Content must have a type"));
            contentJsonObject = contentJsonObject.getJsonObject(type);
          }
          switch (type) {
            case "text":
              return TextContentDataType.create(contentJsonObject);
            case "image":
              return ImageContentDataType.create(contentJsonObject);
            case "audio":
              return AudioContentDataType.create(contentJsonObject);
            case "resourceLink":
            case "resource_link":
              return ResourceLinkContentDataType.create(contentJsonObject);
            default:
              throw new IllegalArgumentException("Content type not supported: " + type);
          }
        }).collect(Collectors.toUnmodifiableList());
      };
    }

    @Override
    default String type() {
      return "unstructured";
    }

    @Override
    default JsonObject toJson() {
      if (content().isEmpty()) {
        return new JsonObject().put("type", type());
      }
      return new JsonObject().put("type", type()).put("content", toJsonArray());
    }

    default JsonArray toJsonArray() {
      JsonArray contentJson = new JsonArray();
      for (ContentDataType content : content()) {
        contentJson.add(content.toJson());
      }
      return contentJson;
    }

    List<ContentDataType> content();
  }

  public interface TextContentDataType extends ContentDataType {
    public static TextContentDataType create(String text) {
      return () -> text;
    }

    public static TextContentDataType create(JsonObject json) {
      return create(json.getString("text"));
    }

    @Override
    default String type() {
      return "text";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject().put("type", type()).put("text", text());
    }

    String text();
  }

  public interface ImageContentDataType extends ContentDataType {
    public static ImageContentDataType create(String data, String mimeType) {
      return new ImageContentDataType() {
        @Override
        public String data() {
          return data;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }
      };
    }

    public static ImageContentDataType create(JsonObject json) {
      return create(json.getString("data"), json.getString("mimeType"));
    }

    @Override
    default String type() {
      return "image";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject()
        .put("type", type())
        .put("data", data())
        .put("mimeType", mimeType());
    }

    String data();

    String mimeType();
  }

  public interface AudioContentDataType extends ContentDataType {
    public static AudioContentDataType create(String data, String mimeType) {
      return new AudioContentDataType() {
        @Override
        public String data() {
          return data;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }
      };
    }

    public static AudioContentDataType create(JsonObject json) {
      return create(json.getString("data"), json.getString("mimeType"));
    }

    @Override
    default String type() {
      return "audio";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject()
        .put("type", type())
        .put("data", data())
        .put("mimeType", mimeType());
    }

    String data();

    String mimeType();
  }

  public interface ResourceLinkContentDataType extends ContentDataType {
    public static ResourceLinkContentDataType create(URI uri, String name, String description, String mimeType) {
      return new ResourceLinkContentDataType() {
        @Override
        public URI uri() {
          return uri;
        }

        @Override
        public String name() {
          return name;
        }

        @Override
        public String description() {
          return description;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }
      };
    }

    public static ResourceLinkContentDataType create(JsonObject json) {
      return create(
        URI.create(json.getString("uri")),
        json.getString("name"),
        json.getString("description"),
        json.getString("mimeType")
      );
    }

    @Override
    default String type() {
      return "resource_link";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject()
        .put("type", type())
        .put("uri", uri().toString())
        .put("name", name())
        .put("description", description())
        .put("mimeType", mimeType());
    }

    URI uri();

    String name();

    String description();

    String mimeType();
  }

  // TODO: Make this work with Resources
  public interface EmbeddedResourceContentDataType extends ContentDataType {
    default String type() {
      return "resource";
    }
  }
}
