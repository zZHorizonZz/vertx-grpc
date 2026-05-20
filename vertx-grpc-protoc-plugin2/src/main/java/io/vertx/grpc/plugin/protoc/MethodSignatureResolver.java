package io.vertx.grpc.plugin.protoc;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import io.vertx.grpc.plugin.descriptors.MethodSignatureDescriptor;
import io.vertx.grpc.plugin.descriptors.MethodSignatureField;
import io.vertx.grpc.plugin.generation.context.NameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves a raw {@code google.api.method_signature} annotation value (a comma-separated string of
 * proto field names) against the input message descriptor of the annotated RPC, producing a
 * {@link MethodSignatureDescriptor} with each field's Java type and builder setter name.
 */
public class MethodSignatureResolver {

  private final ProtobufTypeMapper typeMapper;

  public MethodSignatureResolver(ProtobufTypeMapper typeMapper) {
    this.typeMapper = typeMapper;
  }

  /**
   * Resolves the annotation value against the given input message proto type name (e.g.
   * {@code ".my.pkg.Request"}). Returns {@code null} if the annotation is empty or if any of its
   * field names cannot be resolved against the input message.
   */
  public MethodSignatureDescriptor resolve(String inputProtoTypeName, String annotationValue) {
    if (annotationValue == null) {
      return null;
    }
    String trimmed = annotationValue.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    DescriptorProto inputMessage = typeMapper.getMessageDescriptor(inputProtoTypeName);
    if (inputMessage == null) {
      return null;
    }
    List<MethodSignatureField> resolved = new ArrayList<>();
    for (String rawName : Arrays.asList(trimmed.split(","))) {
      String fieldName = rawName.trim();
      if (fieldName.isEmpty()) {
        return null;
      }
      FieldDescriptorProto field = findField(inputMessage, fieldName);
      if (field == null) {
        return null;
      }
      resolved.add(toSignatureField(field));
    }
    return new MethodSignatureDescriptor(resolved);
  }

  private FieldDescriptorProto findField(DescriptorProto message, String name) {
    for (FieldDescriptorProto field : message.getFieldList()) {
      if (field.getName().equals(name)) {
        return field;
      }
    }
    return null;
  }

  private MethodSignatureField toSignatureField(FieldDescriptorProto field) {
    String upperCamel = toUpperCamel(field.getName());
    String javaName = NameUtils.formatMethodName(field.getName());

    if (isMap(field)) {
      DescriptorProto entry = typeMapper.getMessageDescriptor(field.getTypeName());
      FieldDescriptorProto keyField = entry.getField(0);
      FieldDescriptorProto valueField = entry.getField(1);
      String keyType = boxedJavaType(keyField);
      String valueType = boxedJavaType(valueField);
      String javaType = "java.util.Map<" + keyType + ", " + valueType + ">";
      return new MethodSignatureField(field.getName(), javaName, javaType, "putAll" + upperCamel);
    }
    if (field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED) {
      String elementType = boxedJavaType(field);
      String javaType = "java.util.List<" + elementType + ">";
      return new MethodSignatureField(field.getName(), javaName, javaType, "addAll" + upperCamel);
    }
    String javaType = javaTypeFor(field);
    return new MethodSignatureField(field.getName(), javaName, javaType, "set" + upperCamel);
  }

  private boolean isMap(FieldDescriptorProto field) {
    if (field.getLabel() != FieldDescriptorProto.Label.LABEL_REPEATED) {
      return false;
    }
    if (field.getType() != FieldDescriptorProto.Type.TYPE_MESSAGE) {
      return false;
    }
    DescriptorProto entry = typeMapper.getMessageDescriptor(field.getTypeName());
    return entry != null && entry.getOptions().getMapEntry();
  }

  private String javaTypeFor(FieldDescriptorProto field) {
    switch (field.getType()) {
      case TYPE_DOUBLE:
        return "double";
      case TYPE_FLOAT:
        return "float";
      case TYPE_INT64:
      case TYPE_UINT64:
      case TYPE_FIXED64:
      case TYPE_SFIXED64:
      case TYPE_SINT64:
        return "long";
      case TYPE_INT32:
      case TYPE_UINT32:
      case TYPE_FIXED32:
      case TYPE_SFIXED32:
      case TYPE_SINT32:
        return "int";
      case TYPE_BOOL:
        return "boolean";
      case TYPE_STRING:
        return "java.lang.String";
      case TYPE_BYTES:
        return "com.google.protobuf.ByteString";
      case TYPE_MESSAGE:
      case TYPE_ENUM:
      case TYPE_GROUP:
        return typeMapper.toJavaTypeName(field.getTypeName());
      default:
        return field.getType().name();
    }
  }

  private String boxedJavaType(FieldDescriptorProto field) {
    switch (field.getType()) {
      case TYPE_DOUBLE:
        return "java.lang.Double";
      case TYPE_FLOAT:
        return "java.lang.Float";
      case TYPE_INT64:
      case TYPE_UINT64:
      case TYPE_FIXED64:
      case TYPE_SFIXED64:
      case TYPE_SINT64:
        return "java.lang.Long";
      case TYPE_INT32:
      case TYPE_UINT32:
      case TYPE_FIXED32:
      case TYPE_SFIXED32:
      case TYPE_SINT32:
        return "java.lang.Integer";
      case TYPE_BOOL:
        return "java.lang.Boolean";
      case TYPE_STRING:
        return "java.lang.String";
      case TYPE_BYTES:
        return "com.google.protobuf.ByteString";
      case TYPE_MESSAGE:
      case TYPE_ENUM:
      case TYPE_GROUP:
        return typeMapper.toJavaTypeName(field.getTypeName());
      default:
        return field.getType().name();
    }
  }

  private static String toUpperCamel(String snake) {
    StringBuilder sb = new StringBuilder();
    boolean upper = true;
    for (int i = 0; i < snake.length(); i++) {
      char c = snake.charAt(i);
      if (c == '_') {
        upper = true;
      } else if (upper) {
        sb.append(Character.toUpperCase(c));
        upper = false;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
