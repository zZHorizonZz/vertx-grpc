package io.vertx.jrpc.mcp.impl;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonInputSchemaUtil {

  /**
   * Converts a Protobuf Descriptor to a JSON Schema compatible with Vert.x JsonObject
   *
   * @param descriptor The Protobuf descriptor to convert
   * @return JsonObject representing the JSON Schema
   */
  public static JsonObject toJsonSchema(Descriptor descriptor) {
    JsonObject schema = new JsonObject();

    // Set schema version
    schema.put("$schema", "http://json-schema.org/draft-07/schema#");
    schema.put("type", "object");
    schema.put("additionalProperties", false);

    // Create properties object
    JsonObject properties = new JsonObject();

    // Process each field in the descriptor
    for (FieldDescriptor field : descriptor.getFields()) {
      JsonObject fieldSchema = createFieldSchema(field);
      properties.put(field.getJsonName(), fieldSchema);
    }

    schema.put("properties", properties);

    // Add required fields (all non-optional fields in proto3, or required fields in proto2)
    JsonArray required = new JsonArray();
    for (FieldDescriptor field : descriptor.getFields()) {
      if (isRequired(field)) {
        required.add(field.getJsonName());
      }
    }

    if (!required.isEmpty()) {
      schema.put("required", required);
    }

    return schema;
  }

  /**
   * Creates a JSON Schema for a single field
   */
  private static JsonObject createFieldSchema(FieldDescriptor field) {
    JsonObject fieldSchema = new JsonObject();

    /*if (field.getOptions().hasExtension(com.google.protobuf.DescriptorProtos.FieldOptions.D)) {
      fieldSchema.put("description", field.getFullName() + " (deprecated)");
    } else {
      fieldSchema.put("description", "Field: " + field.getName());
    }*/

    fieldSchema.put("description", "Field: " + field.getName());

    // Handle repeated fields
    if (field.isRepeated()) {
      fieldSchema.put("type", "array");
      JsonObject itemSchema = getTypeSchema(field);
      fieldSchema.put("items", itemSchema);
      return fieldSchema;
    }

    // Handle maps
    if (field.isMapField()) {
      fieldSchema.put("type", "object");
      fieldSchema.put("additionalProperties", getTypeSchema(field.getMessageType().findFieldByNumber(2)));
      return fieldSchema;
    }

    // Handle regular fields
    JsonObject typeSchema = getTypeSchema(field);
    fieldSchema.mergeIn(typeSchema);

    // Add default value if present
    if (field.hasDefaultValue()) {
      fieldSchema.put("default", convertDefaultValue(field));
    }

    return fieldSchema;
  }

  /**
   * Gets the JSON Schema type for a field based on its Protobuf type
   */
  private static JsonObject getTypeSchema(FieldDescriptor field) {
    JsonObject schema = new JsonObject();

    switch (field.getType()) {
      case DOUBLE:
      case FLOAT:
        schema.put("type", "number");
        break;

      case UINT64:
      case INT32:
      case FIXED32:
      case UINT32:
      case SFIXED32:
      case SINT32:
        schema.put("type", "integer");
        break;

      case BOOL:
        schema.put("type", "boolean");
        break;

      case INT64:
      case FIXED64:
      case SFIXED64:
      case SINT64:
      case STRING:
        schema.put("type", "string");
        break;

      case BYTES:
        schema.put("type", "string");
        schema.put("format", "byte");
        break;

      case ENUM:
        schema.put("type", "string");
        JsonArray enumValues = new JsonArray();
        field.getEnumType().getValues().forEach(value ->
          enumValues.add(value.getName())
        );
        schema.put("enum", enumValues);
        break;

      case MESSAGE:
        // For nested messages, recursively convert
        return toJsonSchema(field.getMessageType());

      default:
        schema.put("type", "string");
    }

    return schema;
  }

  /**
   * Converts default values from Protobuf to JSON Schema format
   */
  private static Object convertDefaultValue(FieldDescriptor field) {
    Object defaultValue = field.getDefaultValue();

    switch (field.getType()) {
      case BYTES:
        // Convert ByteString to base64 string
        return java.util.Base64.getEncoder().encodeToString(
          ((com.google.protobuf.ByteString) defaultValue).toByteArray()
        );
      case ENUM:
        // Convert enum to string name
        return defaultValue.toString();
      default:
        return defaultValue;
    }
  }

  /**
   * Determines if a field should be marked as required
   */
  private static boolean isRequired(FieldDescriptor field) {
    // In proto3, only repeated fields and messages are truly optional
    // In proto2, check if field is required
    /*if (field.getFile().getSyntax() == com.google.protobuf.Descriptors.FileDescriptor.Syntax.PROTO2) {
      return field.isRequired();
    } else {
      // Proto3: scalar fields are always present (have defaults)
      // Only messages and repeated fields are truly optional
      return false; // You might want to adjust this based on your needs
    }*/
    return false;
  }

  /**
   * Helper method to create a schema with custom options
   */
  public static JsonObject toJsonSchema(Descriptor descriptor, JsonObject options) {
    JsonObject schema = toJsonSchema(descriptor);

    // Apply custom options if provided
    if (options != null) {
      if (options.containsKey("title")) {
        schema.put("title", options.getString("title"));
      }
      if (options.containsKey("description")) {
        schema.put("description", options.getString("description"));
      }
      if (options.containsKey("additionalProperties")) {
        schema.put("additionalProperties", options.getBoolean("additionalProperties"));
      }
    }

    return schema;
  }
}
