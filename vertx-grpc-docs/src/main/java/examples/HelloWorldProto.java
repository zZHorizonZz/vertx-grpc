// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: helloworld.proto
// Protobuf Java Version: 4.29.3

package examples;

public final class HelloWorldProto {
  private HelloWorldProto() {}
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 29,
      /* patch= */ 3,
      /* suffix= */ "",
      HelloWorldProto.class.getName());
  }
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_HelloRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_helloworld_HelloRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_HelloReply_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_helloworld_HelloReply_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020helloworld.proto\022\nhelloworld\"\034\n\014HelloR" +
      "equest\022\014\n\004name\030\001 \001(\t\"\035\n\nHelloReply\022\017\n\007me" +
      "ssage\030\001 \001(\t2I\n\007Greeter\022>\n\010SayHello\022\030.hel" +
      "loworld.HelloRequest\032\026.helloworld.HelloR" +
      "eply\"\000B#\n\010examplesB\017HelloWorldProtoP\001\242\002\003" +
      "HLWb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_helloworld_HelloRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_helloworld_HelloRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_helloworld_HelloRequest_descriptor,
        new java.lang.String[] { "Name", });
    internal_static_helloworld_HelloReply_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_helloworld_HelloReply_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_helloworld_HelloReply_descriptor,
        new java.lang.String[] { "Message", });
    descriptor.resolveAllFeaturesImmutable();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
