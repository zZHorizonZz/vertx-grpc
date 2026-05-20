package io.vertx.grpc.plugin.descriptors;

/**
 * A single field within a {@link MethodSignatureDescriptor}: the protobuf field as it appears in the
 * method's flattened overload, with its Java parameter name, parameter type, and builder setter
 * name resolved.
 */
public class MethodSignatureField {

  private final String protoFieldName;
  private final String javaName;
  private final String javaType;
  private final String setterName;

  public MethodSignatureField(String protoFieldName, String javaName, String javaType, String setterName) {
    this.protoFieldName = protoFieldName;
    this.javaName = javaName;
    this.javaType = javaType;
    this.setterName = setterName;
  }

  /**
   * The original proto field name (snake_case), as written in the {@code method_signature} annotation.
   */
  public String getProtoFieldName() {
    return protoFieldName;
  }

  /**
   * The Java parameter name (lowerCamelCase), used in both the method signature and the builder call.
   */
  public String getJavaName() {
    return javaName;
  }

  /**
   * The Java parameter type, e.g. {@code "String"}, {@code "java.util.List<String>"}, or a fully-qualified
   * message type.
   */
  public String getJavaType() {
    return javaType;
  }

  /**
   * The builder method that consumes this parameter, e.g. {@code "setName"}, {@code "addAllTags"}, or
   * {@code "putAllAttributes"}.
   */
  public String getSetterName() {
    return setterName;
  }
}
