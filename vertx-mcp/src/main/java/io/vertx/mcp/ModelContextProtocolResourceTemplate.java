package io.vertx.mcp;

public interface ModelContextProtocolResourceTemplate {
  static ModelContextProtocolResourceTemplate create(String name, String title, String description, String uriTemplate, String mimeType) {
    return new ModelContextProtocolResourceTemplate() {
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
      public String uriTemplate() {
        return uriTemplate;
      }

      @Override
      public String mimeType() {
        return mimeType;
      }
    };
  }

  String name();

  String title();

  String description();

  String uriTemplate();

  String mimeType();
}
