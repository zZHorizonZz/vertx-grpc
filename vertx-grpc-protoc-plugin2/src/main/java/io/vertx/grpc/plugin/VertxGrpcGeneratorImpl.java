/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.plugin;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VertxGrpcGeneratorImpl extends Generator {

  private static final int SERVICE_NUMBER_OF_PATHS = 2;
  private static final int METHOD_NUMBER_OF_PATHS = 4;

  private final boolean generateGrpcClient;
  private final boolean generateGrpcService;

  public VertxGrpcGeneratorImpl(boolean generateGrpcClient, boolean generateGrpcService) {
    this.generateGrpcClient = generateGrpcClient;
    this.generateGrpcService = generateGrpcService;
  }

  private String getServiceJavaDocPrefix() {
    return "    ";
  }

  private String getMethodJavaDocPrefix() {
    return "        ";
  }

  @Override
  protected List<PluginProtos.CodeGeneratorResponse.Feature> supportedFeatures() {
    return Collections.singletonList(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL);
  }

  @Override
  public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
    ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

    List<DescriptorProtos.FileDescriptorProto> protosToGenerate = request.getProtoFileList().stream()
      .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
      .collect(Collectors.toList());

    List<ServiceContext> services = findServices(protosToGenerate, typeMap);
    return generateFiles(services);
  }

  private List<ServiceContext> findServices(List<DescriptorProtos.FileDescriptorProto> protos, ProtoTypeMap typeMap) {
    List<ServiceContext> contexts = new ArrayList<>();

    protos.forEach(fileProto -> {
      for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
        ServiceContext serviceContext = buildServiceContext(
          fileProto.getService(serviceNumber),
          typeMap,
          fileProto.getSourceCodeInfo().getLocationList(),
          serviceNumber
        );
        serviceContext.protoName = fileProto.getName();
        serviceContext.packageName = fileProto.getPackage();
        serviceContext.outerClassName = ProtoTypeMap.getJavaOuterClassname(fileProto);
        serviceContext.javaPackageName = extractPackageName(fileProto);
        contexts.add(serviceContext);
      }
    });

    return contexts;
  }

  private String extractPackageName(DescriptorProtos.FileDescriptorProto proto) {
    DescriptorProtos.FileOptions options = proto.getOptions();
    if (options != null) {
      String javaPackage = options.getJavaPackage();
      if (!Strings.isNullOrEmpty(javaPackage)) {
        return javaPackage;
      }
    }

    return Strings.nullToEmpty(proto.getPackage());
  }

  private ServiceContext buildServiceContext(DescriptorProtos.ServiceDescriptorProto serviceProto, ProtoTypeMap typeMap, List<DescriptorProtos.SourceCodeInfo.Location> locations, int serviceNumber) {
    ServiceContext serviceContext = new ServiceContext();
    // Set Later
    //serviceContext.fileName = CLASS_PREFIX + serviceProto.getName() + "Grpc.java";
    //serviceContext.className = CLASS_PREFIX + serviceProto.getName() + "Grpc";
    serviceContext.serviceName = serviceProto.getName();
    serviceContext.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

    List<DescriptorProtos.SourceCodeInfo.Location> allLocationsForService = locations.stream()
      .filter(location ->
        location.getPathCount() >= 2 &&
          location.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER &&
          location.getPath(1) == serviceNumber
      )
      .collect(Collectors.toList());

    DescriptorProtos.SourceCodeInfo.Location serviceLocation = allLocationsForService.stream()
      .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS)
      .findFirst()
      .orElseGet(DescriptorProtos.SourceCodeInfo.Location::getDefaultInstance);
    serviceContext.javaDoc = getJavaDoc(getComments(serviceLocation), getServiceJavaDocPrefix());

    for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
      MethodContext methodContext = buildMethodContext(
        serviceProto.getMethod(methodNumber),
        typeMap,
        locations,
        methodNumber
      );

      serviceContext.methods.add(methodContext);
    }
    return serviceContext;
  }

  private MethodContext buildMethodContext(DescriptorProtos.MethodDescriptorProto methodProto, ProtoTypeMap typeMap, List<DescriptorProtos.SourceCodeInfo.Location> locations, int methodNumber) {
    MethodContext methodContext = new MethodContext();
    methodContext.methodName = methodProto.getName();
    methodContext.vertxMethodName = mixedLower(methodProto.getName());
    methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
    methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
    methodContext.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
    methodContext.isManyInput = methodProto.getClientStreaming();
    methodContext.isManyOutput = methodProto.getServerStreaming();
    methodContext.methodNumber = methodNumber;
    methodContext.transcodingContext = new TranscodingContext();

    DescriptorProtos.SourceCodeInfo.Location methodLocation = locations.stream()
      .filter(location ->
        location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
          location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber
      )
      .findFirst()
      .orElseGet(DescriptorProtos.SourceCodeInfo.Location::getDefaultInstance);
    methodContext.javaDoc = getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix());

    if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
      methodContext.vertxCallsMethodName = "oneToOne";
      methodContext.grpcCallsMethodName = "asyncUnaryCall";
    }
    if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
      methodContext.vertxCallsMethodName = "oneToMany";
      methodContext.grpcCallsMethodName = "asyncServerStreamingCall";
    }
    if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
      methodContext.vertxCallsMethodName = "manyToOne";
      methodContext.grpcCallsMethodName = "asyncClientStreamingCall";
    }
    if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
      methodContext.vertxCallsMethodName = "manyToMany";
      methodContext.grpcCallsMethodName = "asyncBidiStreamingCall";
    }

    if (methodProto.getOptions().hasExtension(AnnotationsProto.http)) {
      HttpRule httpRule = methodProto.getOptions().getExtension(AnnotationsProto.http);
      methodContext.transcodingContext = buildTranscodingContext(httpRule);
    }

    return methodContext;
  }

  private TranscodingContext buildTranscodingContext(HttpRule rule) {
    TranscodingContext transcodingContext = new TranscodingContext();
    switch (rule.getPatternCase()) {
      case GET:
        transcodingContext.path = rule.getGet();
        transcodingContext.method = "GET";
        break;
      case POST:
        transcodingContext.path = rule.getPost();
        transcodingContext.method = "POST";
        break;
      case PUT:
        transcodingContext.path = rule.getPut();
        transcodingContext.method = "PUT";
        break;
      case DELETE:
        transcodingContext.path = rule.getDelete();
        transcodingContext.method = "DELETE";
        break;
      case PATCH:
        transcodingContext.path = rule.getPatch();
        transcodingContext.method = "PATCH";
        break;
      case CUSTOM:
        transcodingContext.path = rule.getCustom().getPath();
        transcodingContext.method = rule.getCustom().getKind();
        break;
    }

    transcodingContext.selector = rule.getSelector();
    transcodingContext.body = rule.getBody();
    transcodingContext.responseBody = rule.getResponseBody();

    transcodingContext.additionalBindings = rule.getAdditionalBindingsList().stream()
      .map(this::buildTranscodingContext)
      .collect(Collectors.toList());

    return transcodingContext;
  }

  // java keywords from: https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.9
  private static final List<CharSequence> JAVA_KEYWORDS = Arrays.asList(
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
    // additional ones added by us
    "true",
    "false"
  );

  /**
   * Adjust a method name prefix identifier to follow the JavaBean spec:
   * - decapitalize the first letter
   * - remove embedded underscores & capitalize the following letter
   * <p>
   * Finally, if the result is a reserved java keyword, append an underscore.
   *
   * @param word method name
   * @return lower name
   */
  private static String mixedLower(String word) {
    StringBuffer w = new StringBuffer();
    w.append(Character.toLowerCase(word.charAt(0)));

    boolean afterUnderscore = false;

    for (int i = 1; i < word.length(); ++i) {
      char c = word.charAt(i);

      if (c == '_') {
        afterUnderscore = true;
      } else {
        if (afterUnderscore) {
          w.append(Character.toUpperCase(c));
        } else {
          w.append(c);
        }
        afterUnderscore = false;
      }
    }

    if (JAVA_KEYWORDS.contains(w)) {
      w.append('_');
    }

    return w.toString();
  }

  private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(List<ServiceContext> services) {
    List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();
    return services.stream()
      .map(this::buildFiles)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  private List<PluginProtos.CodeGeneratorResponse.File> buildFiles(ServiceContext context) {
    List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();
    files.add(buildBaseFile(context));
    if (generateGrpcClient) {
      files.add(buildClientFile(context));
      files.add(buildGrpcClientFile(context));
    }
    if (generateGrpcService) {
      files.add(buildServiceFile(context));
      files.add(buildGrpcServiceFile(context));
    }
    return files;
  }

  private PluginProtos.CodeGeneratorResponse.File buildBaseFile(ServiceContext context) {
    context.fileName = context.serviceName + ".java";
    context.className = context.serviceName;
    return buildFile(context, applyTemplate("base.mustache", context));
  }

  private PluginProtos.CodeGeneratorResponse.File buildClientFile(ServiceContext context) {
    context.fileName = context.serviceName + "Client.java";
    context.className = context.serviceName + "Client";
    return buildFile(context, applyTemplate("client.mustache", context));
  }

  private PluginProtos.CodeGeneratorResponse.File buildGrpcClientFile(ServiceContext context) {
    context.fileName = context.serviceName + "GrpcClient.java";
    context.className = context.serviceName + "GrpcClient";
    return buildFile(context, applyTemplate("grpc-client.mustache", context));
  }

  private PluginProtos.CodeGeneratorResponse.File buildServiceFile(ServiceContext context) {
    context.fileName = context.serviceName + "Service.java";
    context.className = context.serviceName + "Service";
    return buildFile(context, applyTemplate("service.mustache", context));
  }

  private PluginProtos.CodeGeneratorResponse.File buildGrpcServiceFile(ServiceContext context) {
    context.fileName = context.serviceName + "GrpcService.java";
    context.className = context.serviceName + "GrpcService";
    return buildFile(context, applyTemplate("grpc-service.mustache", context));
  }

  private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context, String content) {
    return PluginProtos.CodeGeneratorResponse.File
      .newBuilder()
      .setName(absoluteFileName(context))
      .setContent(content)
      .build();
  }

  private String absoluteFileName(ServiceContext ctx) {
    String dir = ctx.javaPackageName.replace('.', '/');
    if (Strings.isNullOrEmpty(dir)) {
      return ctx.fileName;
    } else {
      return dir + "/" + ctx.fileName;
    }
  }

  private String getComments(DescriptorProtos.SourceCodeInfo.Location location) {
    return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
  }

  private String getJavaDoc(String comments, String prefix) {
    if (!comments.isEmpty()) {
      StringBuilder builder = new StringBuilder("/**\n")
        .append(prefix).append(" * <pre>\n");
      Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
        .map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
        .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
      builder
        .append(prefix).append(" * </pre>\n")
        .append(prefix).append(" */");
      return builder.toString();
    }
    return null;
  }

  /**
   * Template class for proto Service objects.
   */
  private static class ServiceContext {
    // CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
    public String fileName;
    public String protoName;
    public String packageName;
    public String javaPackageName;
    public String className;
    public String serviceName;
    public String outerClassName;
    public boolean deprecated;
    public String javaDoc;
    public final List<MethodContext> methods = new ArrayList<>();

    public List<MethodContext> allMethods() {
      return methods;
    }

    public List<MethodContext> streamMethods() {
      return methods.stream().filter(m -> m.isManyInput || m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> unaryUnaryMethods() {
      return methods.stream().filter(m -> !m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> unaryManyMethods() {
      return methods.stream().filter(m -> !m.isManyInput && m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> manyUnaryMethods() {
      return methods.stream().filter(m -> m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> manyManyMethods() {
      return methods.stream().filter(m -> m.isManyInput && m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> transcodingMethods() {
      return methods.stream().filter(t -> t.transcodingContext != null && t.transcodingContext.path != null).collect(Collectors.toList());
    }
  }

  /**
   * Template class for proto RPC objects.
   */
  private static class MethodContext {
    // CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
    public String methodName;
    public String vertxMethodName;
    public String inputType;
    public String outputType;
    public boolean deprecated;
    public boolean isManyInput;
    public boolean isManyOutput;
    public String vertxCallsMethodName;
    public String grpcCallsMethodName;
    public int methodNumber;
    public String javaDoc;

    public TranscodingContext transcodingContext;

    // This method mimics the upper-casing method ogf gRPC to ensure compatibility
    // See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
    public String methodNameUpperUnderscore() {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < methodName.length(); i++) {
        char c = methodName.charAt(i);
        s.append(Character.toUpperCase(c));
        if ((i < methodName.length() - 1) && Character.isLowerCase(c) && Character.isUpperCase(methodName.charAt(i + 1))) {
          s.append('_');
        }
      }
      return s.toString();
    }

    public String methodNameGetter() {
      return VertxGrpcGeneratorImpl.mixedLower("get_" + methodName + "_method");
    }

    public String methodHeader() {
      String mh = "";
      if (!Strings.isNullOrEmpty(javaDoc)) {
        mh = javaDoc;
      }

      if (deprecated) {
        mh += "\n        @Deprecated";
      }

      return mh;
    }
  }

  private static class TranscodingContext {
    public String path;
    public String method;
    public String selector;
    public String body;
    public String responseBody;
    public List<TranscodingContext> additionalBindings = new ArrayList<>();
  }
}
