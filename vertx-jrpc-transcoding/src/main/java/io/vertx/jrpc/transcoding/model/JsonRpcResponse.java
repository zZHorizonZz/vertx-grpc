/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.jrpc.transcoding.model;

import io.vertx.core.json.JsonObject;

/**
 * Represents a JSON-RPC 2.0 response object.
 * <p>
 * A JSON-RPC response contains:
 * <ul>
 *   <li>jsonrpc - A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0".</li>
 *   <li>result - This member is REQUIRED on success. This member MUST NOT exist if there was an error invoking the method.</li>
 *   <li>error - This member is REQUIRED on error. This member MUST NOT exist if there was no error triggered during invocation.</li>
 *   <li>id - This member is REQUIRED. It MUST be the same as the value of the id member in the Request Object.</li>
 * </ul>
 */
public class JsonRpcResponse {

  private static final String JSONRPC_VERSION = "2.0";
  private static final String JSONRPC_FIELD = "jsonrpc";
  private static final String RESULT_FIELD = "result";
  private static final String ERROR_FIELD = "error";
  private static final String ID_FIELD = "id";

  private final String jsonrpc;

  private final Object id;
  private final Object result;
  private final JsonRpcError error;

  /**
   * Creates a new JSON-RPC success response.
   *
   * @param result the result value
   * @param id the request identifier (must match the request id)
   */
  public JsonRpcResponse(Object result, Object id) {
    this.jsonrpc = JSONRPC_VERSION;
    this.result = result;
    this.error = null;
    this.id = id;
  }

  /**
   * Creates a new JSON-RPC error response.
   *
   * @param error the error object
   * @param id the request identifier (must match the request id, or null if the id could not be determined)
   */
  public JsonRpcResponse(JsonRpcError error, Object id) {
    this.jsonrpc = JSONRPC_VERSION;
    this.result = null;
    this.error = error;
    this.id = id;
  }

  /**
   * Creates a success response for the given request.
   *
   * @param request the request
   * @param result the result value
   * @return a new JSON-RPC success response
   */
  public static JsonRpcResponse success(JsonRpcRequest request, Object result) {
    return new JsonRpcResponse(result, request.getId());
  }

  /**
   * Creates an error response for the given request.
   *
   * @param request the request (can be null if the request could not be parsed)
   * @param error the error object
   * @return a new JSON-RPC error response
   */
  public static JsonRpcResponse error(JsonRpcRequest request, JsonRpcError error) {
    return new JsonRpcResponse(error, request != null ? request.getId() : null);
  }

  /**
   * Creates a JSON-RPC response from a JsonObject.
   *
   * @param json the JsonObject representing the response
   * @return a new JsonRpcResponse
   * @throws IllegalArgumentException if the JsonObject is not a valid JSON-RPC response
   */
  public static JsonRpcResponse fromJson(JsonObject json) {
    String version = json.getString(JSONRPC_FIELD);
    if (version == null || !version.equals(JSONRPC_VERSION)) {
      throw new IllegalArgumentException("Invalid JSON-RPC version: " + version);
    }

    Object id = json.getValue(ID_FIELD);
    // ID is required in a response
    if (id == null && !json.containsKey(ID_FIELD)) {
      throw new IllegalArgumentException("Id is required");
    }

    boolean hasResult = json.containsKey(RESULT_FIELD);
    boolean hasError = json.containsKey(ERROR_FIELD);

    // Either result or error must be present, but not both
    if (hasResult && hasError) {
      throw new IllegalArgumentException("Response cannot contain both result and error");
    }
    if (!hasResult && !hasError) {
      throw new IllegalArgumentException("Response must contain either result or error");
    }

    if (hasResult) {
      return new JsonRpcResponse(json.getValue(RESULT_FIELD), id);
    } else {
      JsonObject errorObj = json.getJsonObject(ERROR_FIELD);
      if (errorObj == null) {
        throw new IllegalArgumentException("Error must be an object");
      }
      return new JsonRpcResponse(JsonRpcError.fromJson(errorObj), id);
    }
  }

  /**
   * Converts this response to a JsonObject.
   *
   * @return the JsonObject representation of this response
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put(JSONRPC_FIELD, jsonrpc)
      .put(ID_FIELD, id);

    if (error != null) {
      json.put(ERROR_FIELD, error.toJson());
    } else {
      json.put(RESULT_FIELD, result);
    }

    return json;
  }

  /**
   * @return the JSON-RPC version
   */
  public String getJsonrpc() {
    return jsonrpc;
  }

  /**
   * @return the result value (null if this is an error response)
   */
  public Object getResult() {
    return result;
  }

  /**
   * @return the error object (null if this is a success response)
   */
  public JsonRpcError getError() {
    return error;
  }

  /**
   * @return the request identifier
   */
  public Object getId() {
    return id;
  }

  /**
   * @return true if this is a success response
   */
  public boolean isSuccess() {
    return error == null;
  }
}
