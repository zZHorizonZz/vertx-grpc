package io.vertx.grpc.plugin.descriptors;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single {@code (google.api.method_signature) = "a,b,c"} annotation: an ordered list of
 * request-message fields that should be exposed as a flattened method overload on the generated
 * client.
 */
public class MethodSignatureDescriptor {

  private final List<MethodSignatureField> fields;

  public MethodSignatureDescriptor(List<MethodSignatureField> fields) {
    this.fields = new ArrayList<>(fields);
  }

  public List<MethodSignatureField> getFields() {
    return fields;
  }

  /**
   * Renders the Java parameter list, e.g. {@code "String name, java.util.List<String> tags"}.
   */
  public String getParamList() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      MethodSignatureField field = fields.get(i);
      sb.append(field.getJavaType()).append(' ').append(field.getJavaName());
    }
    return sb.toString();
  }

  /**
   * Renders the protobuf builder chain that consumes the parameters, e.g.
   * {@code ".setName(name).addAllTags(tags)"}.
   */
  public String getBuilderChain() {
    StringBuilder sb = new StringBuilder();
    for (MethodSignatureField field : fields) {
      sb.append('.').append(field.getSetterName()).append('(').append(field.getJavaName()).append(')');
    }
    return sb.toString();
  }
}
