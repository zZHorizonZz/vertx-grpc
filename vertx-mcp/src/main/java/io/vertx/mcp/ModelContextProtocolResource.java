package io.vertx.mcp;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.net.URI;

public interface ModelContextProtocolResource {
  URI uri();

  String type();

  String name();

  String title();

  String description();

  String mimeType();

  Future<Buffer> content();

  JsonObject toJson();

  interface TextResource extends ModelContextProtocolResource {

    static ModelContextProtocolResource.TextResource create(URI uri, String name, String title, String description, String mimeType, Future<String> text) {
      return new ModelContextProtocolResource.TextResource() {
        @Override
        public URI uri() {
          return uri;
        }

        @Override
        public String name() {
          return name;
        }

        @Override
        public String title() {
          return title;
        }

        @Override
        public String description() {
          return description;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }

        @Override
        public Future<Buffer> content() {
          return text.map(Buffer::buffer);
        }

        @Override
        public Future<String> text() {
          return text;
        }

        @Override
        public JsonObject toJson() {
          return new JsonObject()
            .put("uri", uri.toString())
            .put("name", name)
            .put("title", title)
            .put("description", description)
            .put("mimeType", mimeType);
        }
      };
    }

    default String type() {
      return "text";
    }

    Future<String> text();
  }

  interface BinaryResource extends ModelContextProtocolResource {
    static ModelContextProtocolResource.BinaryResource create(URI uri, String name, String title, String description, String mimeType, Future<Buffer> blob) {
      return new ModelContextProtocolResource.BinaryResource() {

        @Override
        public URI uri() {
          return uri;
        }

        @Override
        public String name() {
          return name;
        }

        @Override
        public String title() {
          return title;
        }

        @Override
        public String description() {
          return description;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }

        @Override
        public Future<Buffer> content() {
          return blob();
        }

        @Override
        public Future<Buffer> blob() {
          return blob;
        }

        @Override
        public JsonObject toJson() {
          return new JsonObject()
            .put("uri", uri.toString())
            .put("name", name)
            .put("title", title)
            .put("description", description)
            .put("mimeType", mimeType);
        }
      };
    }

    default String type() {
      return "blob";
    }

    Future<Buffer> blob();
  }
}
