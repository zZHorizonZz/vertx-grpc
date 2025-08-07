package io.vertx.mcp;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public interface ModelContextProtocolDataType {
  String type();

  JsonObject toJson();

  interface StructuredJsonContentDataType extends ModelContextProtocolDataType {
    static ModelContextProtocolDataType.StructuredJsonContentDataType create(JsonObject json) {
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

  interface UnstructuredContentDataType extends ModelContextProtocolDataType {
    static ModelContextProtocolDataType.UnstructuredContentDataType create(List<ModelContextProtocolDataType> content) {
      return () -> content;
    }

    static ModelContextProtocolDataType.UnstructuredContentDataType create(JsonObject json) {
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
              return ModelContextProtocolDataType.TextContentDataType.create(contentJsonObject);
            case "image":
              return ModelContextProtocolDataType.ImageContentDataType.create(contentJsonObject);
            case "audio":
              return ModelContextProtocolDataType.AudioContentDataType.create(contentJsonObject);
            case "resourceLink":
            case "resource_link":
              return ModelContextProtocolDataType.ResourceLinkContentDataType.create(contentJsonObject);
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
      for (ModelContextProtocolDataType content : content()) {
        contentJson.add(content.toJson());
      }
      return contentJson;
    }

    List<ModelContextProtocolDataType> content();
  }

  interface TextContentDataType extends ModelContextProtocolDataType {
    static ModelContextProtocolDataType.TextContentDataType create(String text) {
      return () -> text;
    }

    static ModelContextProtocolDataType.TextContentDataType create(JsonObject json) {
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

  interface ImageContentDataType extends ModelContextProtocolDataType {
    static ModelContextProtocolDataType.ImageContentDataType create(String data, String mimeType) {
      return new ModelContextProtocolDataType.ImageContentDataType() {
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

    static ModelContextProtocolDataType.ImageContentDataType create(JsonObject json) {
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

  interface AudioContentDataType extends ModelContextProtocolDataType {
    static ModelContextProtocolDataType.AudioContentDataType create(String data, String mimeType) {
      return new ModelContextProtocolDataType.AudioContentDataType() {
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

    static ModelContextProtocolDataType.AudioContentDataType create(JsonObject json) {
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

  interface ResourceLinkContentDataType extends ModelContextProtocolDataType {
    static ModelContextProtocolDataType.ResourceLinkContentDataType create(URI uri, String name, String description, String mimeType) {
      return new ModelContextProtocolDataType.ResourceLinkContentDataType() {
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

    static ModelContextProtocolDataType.ResourceLinkContentDataType create(JsonObject json) {
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
  interface EmbeddedResourceContentDataType extends ModelContextProtocolDataType {
    default String type() {
      return "resource";
    }
  }
}
