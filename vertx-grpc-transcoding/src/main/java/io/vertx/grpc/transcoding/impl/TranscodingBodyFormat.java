package io.vertx.grpc.transcoding.impl;

import com.google.protobuf.Descriptors;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.transcoding.impl.config.HttpVariableBinding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The encoding of a transcoded HTTP request body. This is the HTTP content-type of the request, as opposed to the gRPC
 * {@link WireFormat} of the transcoded message.
 * <p>
 * Each format owns how it turns an HTTP request body (together with the path/query variable bindings) into the buffer
 * handed to the gRPC message decoder. New content-types are added by implementing this interface and registering an
 * instance in {@link #VALUES}, without touching the request routing.
 */
public interface TranscodingBodyFormat {

  /**
   * {@code application/json} request body, woven directly into the gRPC message.
   */
  TranscodingBodyFormat JSON = new JsonBodyFormat();

  /**
   * {@code application/x-www-form-urlencoded} request body, whose fields are treated like query parameters.
   */
  TranscodingBodyFormat FORM_URL_ENCODED = new FormUrlEncodedBodyFormat();

  /**
   * The registered formats, in priority order.
   */
  List<TranscodingBodyFormat> VALUES = Collections.unmodifiableList(Arrays.asList(JSON, FORM_URL_ENCODED));

  /**
   * @return the HTTP content-type this body format maps to
   */
  String contentType();

  /**
   * @return the gRPC wire format of the message produced by {@link #transcode}
   */
  WireFormat wireFormat();

  /**
   * Transcodes an HTTP request body and its path/query variable bindings into the payload consumed by the gRPC message
   * decoder.
   *
   * @param body the raw HTTP request body, possibly {@code null} or empty
   * @param bindings the variable bindings extracted from the request path and query
   * @param bodyFieldPath the {@code HttpRule} body field path ({@code "*"} for the root), or {@code null}
   * @param descriptor the target message descriptor, used to identify repeated fields
   * @return the payload to hand to the message decoder
   */
  Buffer transcode(Buffer body, List<HttpVariableBinding> bindings, String bodyFieldPath, Descriptors.Descriptor descriptor);

  /**
   * Maps an HTTP content-type to its transcoding body format, or {@code null} if none handles it.
   */
  static TranscodingBodyFormat fromContentType(String contentType) {
    for (TranscodingBodyFormat format : VALUES) {
      if (format.contentType().equals(contentType)) {
        return format;
      }
    }
    return null;
  }

  /**
   * {@code application/json} body format.
   */
  final class JsonBodyFormat implements TranscodingBodyFormat {

    @Override
    public String contentType() {
      return "application/json";
    }

    @Override
    public WireFormat wireFormat() {
      return WireFormat.JSON;
    }

    @Override
    public Buffer transcode(Buffer body, List<HttpVariableBinding> bindings, String bodyFieldPath, Descriptors.Descriptor descriptor) {
      return MessageWeaver.weaveRequestMessage(body, bindings, bodyFieldPath, descriptor);
    }
  }

  /**
   * {@code application/x-www-form-urlencoded} body format. Form fields are parsed like query parameters, so they reuse the
   * same dotted-path nesting, repeated-field and percent/{@code +} decoding behaviour, then woven into the message.
   */
  final class FormUrlEncodedBodyFormat implements TranscodingBodyFormat {

    private static final String ROOT_LEVEL = "*";

    @Override
    public String contentType() {
      return HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString();
    }

    @Override
    public WireFormat wireFormat() {
      return WireFormat.JSON;
    }

    @Override
    public Buffer transcode(Buffer body, List<HttpVariableBinding> bindings, String bodyFieldPath, Descriptors.Descriptor descriptor) {
      List<HttpVariableBinding> allBindings = new ArrayList<>(bindings);
      allBindings.addAll(parseFormBindings(body, bodyFieldPath));
      return MessageWeaver.weaveRequestMessage(null, allBindings, null, descriptor);
    }

    /**
     * Parses a form-urlencoded body into variable bindings. When the {@code HttpRule} maps the body to a specific field
     * (i.e. {@code body} is not {@code "*"}), the field paths are prefixed so the form fields nest under it.
     */
    private static List<HttpVariableBinding> parseFormBindings(Buffer body, String bodyFieldPath) {
      if (body == null || body.length() == 0) {
        return Collections.emptyList();
      }
      String encoded = body.toString(StandardCharsets.UTF_8);
      List<HttpVariableBinding> formBindings = PathMatcherUtility.extractBindingsFromQueryParameters(encoded, Collections.emptySet(), true);

      boolean rooted = bodyFieldPath == null || bodyFieldPath.isEmpty() || ROOT_LEVEL.equals(bodyFieldPath);
      if (rooted) {
        return formBindings;
      }

      List<String> prefix = Arrays.asList(bodyFieldPath.split("\\."));
      List<HttpVariableBinding> prefixed = new ArrayList<>(formBindings.size());
      for (HttpVariableBinding binding : formBindings) {
        List<String> fieldPath = new ArrayList<>(prefix);
        fieldPath.addAll(binding.getFieldPath());
        prefixed.add(new HttpVariableBinding(fieldPath, binding.getValue()));
      }
      return prefixed;
    }
  }
}
