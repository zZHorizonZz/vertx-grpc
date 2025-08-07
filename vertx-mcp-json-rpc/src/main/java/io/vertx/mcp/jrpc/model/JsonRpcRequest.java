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
package io.vertx.mcp.jrpc.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Represents a JSON-RPC 2.0 request object.
 * <p>
 * A JSON-RPC request contains:
 * <ul>
 *   <li>jsonrpc - A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0".</li>
 *   <li>method - A String containing the name of the method to be invoked.</li>
 *   <li>params - A Structured value that holds the parameter values (optional).</li>
 *   <li>id - An identifier established by the Client (optional for notifications).</li>
 * </ul>
 */
public class JsonRpcRequest {

  private static final String JSONRPC_VERSION = "2.0";
  private static final String JSONRPC_FIELD = "jsonrpc";
  private static final String METHOD_FIELD = "method";
  private static final String PARAMS_FIELD = "params";
  private static final String ID_FIELD = "id";

  private final Integer id;
  private final String method;
  private final String jsonrpc;

  private final JsonArray unamedParams;
  private final JsonObject namedParams;

  public JsonRpcRequest(String method, JsonArray unamedParams, Integer id) {
    this.jsonrpc = JSONRPC_VERSION;
    this.method = method;
    this.unamedParams = unamedParams;
    this.namedParams = null;
    this.id = id;
  }

  public JsonRpcRequest(String method, JsonObject namedParams, Integer id) {
    this.jsonrpc = JSONRPC_VERSION;
    this.method = method;
    this.unamedParams = null;
    this.namedParams = namedParams;
    this.id = id;
  }

  /**
   * Creates a new JSON-RPC notification (request without an id).
   *
   * @param method the method name
   * @param params the parameters (can be JsonObject for named parameters or JsonArray for positional parameters)
   * @return a new JSON-RPC notification
   */
  public static JsonRpcRequest createNotification(String method, Object params) {
    if (params instanceof JsonObject) {
      return new JsonRpcRequest(method, (JsonObject) params, null);
    } else if (params instanceof JsonArray) {
      return new JsonRpcRequest(method, (JsonArray) params, null);
    } else {
      throw new IllegalArgumentException("Params must be an object or array");
    }
  }

  public static JsonRpcRequest createRequest(String method, Object params, Integer id) {
    if (params instanceof JsonObject) {
      return new JsonRpcRequest(method, (JsonObject) params, id);
    } else if (params instanceof JsonArray) {
      return new JsonRpcRequest(method, (JsonArray) params, id);
    } else {
      throw new IllegalArgumentException("Params must be an object or array");
    }
  }

  /**
   * Creates a JSON-RPC request from a JsonObject.
   *
   * @param json the JsonObject representing the request
   * @return a new JsonRpcRequest
   * @throws IllegalArgumentException if the JsonObject is not a valid JSON-RPC request
   */
  public static JsonRpcRequest fromJson(JsonObject json) {
    String version = json.getString(JSONRPC_FIELD);
    if (version == null || !version.equals(JSONRPC_VERSION)) {
      throw new IllegalArgumentException("Invalid JSON-RPC version: " + version);
    }

    String method = json.getString(METHOD_FIELD);
    if (method == null) {
      throw new IllegalArgumentException("Method is required");
    }

    Object params = null;
    if (json.containsKey(PARAMS_FIELD)) {
      params = json.getValue(PARAMS_FIELD);
      if (!(params instanceof JsonObject) && !(params instanceof JsonArray)) {
        throw new IllegalArgumentException("Params must be an object or array");
      }
    }

    Integer id = null;
    if (json.containsKey(ID_FIELD)) {
      id = json.getInteger(ID_FIELD);
    }

    return createRequest(method, params, id);
  }

  /**
   * Converts this request to a JsonObject.
   *
   * @return the JsonObject representation of this request
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put(JSONRPC_FIELD, jsonrpc)
      .put(METHOD_FIELD, method);

    if (unamedParams != null) {
      json.put(PARAMS_FIELD, unamedParams);
    }

    if (namedParams != null) {
      json.put(PARAMS_FIELD, namedParams);
    }

    if (id != null) {
      json.put(ID_FIELD, id);
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
   * @return the method name
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return the parameters (can be JsonObject, JsonArray, or null)
   */
  public Object getParams() {
    if (unamedParams != null) {
      return unamedParams;
    } else {
      return namedParams;
    }
  }

  public JsonArray getUnnamedParams() {
    return unamedParams;
  }

  public JsonObject getNamedParams() {
    return namedParams;
  }

  public Buffer toBuffer() {
    if (unamedParams != null) {
      return Buffer.buffer(unamedParams.encode());
    } else if (namedParams != null) {
      return Buffer.buffer(namedParams.encode());
    } else {
      return Buffer.buffer();
    }
  }

  /**
   * @return the request identifier (can be String, Number, or null for notifications)
   */
  public Integer getId() {
    return id;
  }

  /**
   * @return true if this request is a notification (no id)
   */
  public boolean isNotification() {
    return id == null;
  }
}
