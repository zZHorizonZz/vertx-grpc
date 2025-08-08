package io.vertx.mcp.bridge.grpc.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.vertx.mcp.proto.*;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;

public class SchemaUtil {

  public static final Descriptors.Descriptor TEXT_CONTENT_DESCRIPTOR = TextContent.getDescriptor();
  public static final Descriptors.Descriptor IMAGE_CONTENT_DESCRIPTOR = ImageContent.getDescriptor();
  public static final Descriptors.Descriptor AUDIO_CONTENT_DESCRIPTOR = AudioContent.getDescriptor();
  public static final Descriptors.Descriptor RESOURCE_LINK_CONTENT_DESCRIPTOR = ResourceLinkContent.getDescriptor();

  public static final Descriptors.Descriptor CONTENT_DESCRIPTOR = Content.getDescriptor();

  /**
   * Converts a Protobuf Descriptor to a Vert.x JsonSchema
   *
   * @param descriptor The Protobuf descriptor to convert
   * @return JsonSchema representing the JSON Schema
   */
  public static JsonSchema toJsonSchema(Descriptor descriptor) {
    ObjectSchemaBuilder object = toObjectSchemaBuilder(descriptor);
    return JsonSchema.of(object.toJson());
  }

  private static ObjectSchemaBuilder toObjectSchemaBuilder(Descriptor descriptor) {
    ObjectSchemaBuilder object = Schemas.objectSchema();
    for (FieldDescriptor field : descriptor.getFields()) {
      SchemaBuilder fieldSchema = createFieldSchema(field);
      if (isRequired(field)) {
        object.requiredProperty(field.getJsonName(), fieldSchema);
      } else {
        object.property(field.getJsonName(), fieldSchema);
      }
    }
    return object;
  }

  /**
   * Creates a JSON Schema for a single field using the Vert.x JSON Schema DSL
   */
  private static SchemaBuilder createFieldSchema(FieldDescriptor field) {
    SchemaBuilder base;

    // Description (applies to all types)
    String description = "Field: " + field.getName();

    // Handle repeated fields (arrays)
    if (field.isRepeated()) {
      SchemaBuilder itemSchema = getTypeSchema(field);
      ArraySchemaBuilder array = Schemas.arraySchema().items(itemSchema);
      return array;
    }

    // Handle maps: protobuf map<K,V> is represented as a message with key/value fields; we only model additionalProperties as value type
    if (field.isMapField()) {
      FieldDescriptor valueField = field.getMessageType().findFieldByNumber(2); // 1=key, 2=value
      SchemaBuilder valueSchema = getTypeSchema(valueField);
      ObjectSchemaBuilder mapObj = Schemas.objectSchema().additionalProperties(valueSchema);
      return mapObj;
    }

    // Regular fields
    base = getTypeSchema(field);
    return base;
  }

  /**
   * Gets the JSON Schema type for a field based on its Protobuf type
   */
  private static SchemaBuilder getTypeSchema(FieldDescriptor field) {
    switch (field.getType()) {
      case DOUBLE:
      case FLOAT:
        return Schemas.numberSchema();

      case UINT64:
      case INT32:
      case FIXED32:
      case UINT32:
      case SFIXED32:
      case SINT32:
        return Schemas.intSchema();

      case BOOL:
        return Schemas.booleanSchema();

      case INT64:
      case FIXED64:
      case SFIXED64:
      case SINT64:
      case STRING:
        return Schemas.stringSchema();

      case BYTES:
        // Represent bytes as string
        return Schemas.stringSchema();

      case ENUM:
        // Represent enum as unconstrained string for compatibility
        return Schemas.stringSchema();

      case MESSAGE:
        // For nested messages, recursively convert
        return toObjectSchemaBuilder(field.getMessageType());

      default:
        return Schemas.stringSchema();
    }
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
}
