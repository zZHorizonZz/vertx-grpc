package io.vertx.grpc.server;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter and mapper for {@link io.vertx.grpc.server.GrpcCorsOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.grpc.server.GrpcCorsOptions} original class using Vert.x codegen.
 */
public class GrpcCorsOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, GrpcCorsOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "allowedOrigins":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<java.lang.String> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setAllowedOrigins(list);
          }
          break;
        case "allowedOriginPatterns":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<java.lang.String> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setAllowedOriginPatterns(list);
          }
          break;
        case "allowCredentials":
          if (member.getValue() instanceof Boolean) {
            obj.setAllowCredentials((Boolean)member.getValue());
          }
          break;
        case "allowedHeaders":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<java.lang.String> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setAllowedHeaders(list);
          }
          break;
        case "exposedHeaders":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<java.lang.String> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setExposedHeaders(list);
          }
          break;
        case "maxAgeSeconds":
          if (member.getValue() instanceof Number) {
            obj.setMaxAgeSeconds(((Number)member.getValue()).intValue());
          }
          break;
      }
    }
  }

   static void toJson(GrpcCorsOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(GrpcCorsOptions obj, java.util.Map<String, Object> json) {
    if (obj.getAllowedOrigins() != null) {
      JsonArray array = new JsonArray();
      obj.getAllowedOrigins().forEach(item -> array.add(item));
      json.put("allowedOrigins", array);
    }
    if (obj.getAllowedOriginPatterns() != null) {
      JsonArray array = new JsonArray();
      obj.getAllowedOriginPatterns().forEach(item -> array.add(item));
      json.put("allowedOriginPatterns", array);
    }
    json.put("allowCredentials", obj.getAllowCredentials());
    if (obj.getAllowedHeaders() != null) {
      JsonArray array = new JsonArray();
      obj.getAllowedHeaders().forEach(item -> array.add(item));
      json.put("allowedHeaders", array);
    }
    if (obj.getExposedHeaders() != null) {
      JsonArray array = new JsonArray();
      obj.getExposedHeaders().forEach(item -> array.add(item));
      json.put("exposedHeaders", array);
    }
    json.put("maxAgeSeconds", obj.getMaxAgeSeconds());
  }
}
