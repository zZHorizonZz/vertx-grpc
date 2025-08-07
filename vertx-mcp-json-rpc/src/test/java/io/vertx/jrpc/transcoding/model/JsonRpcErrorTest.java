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
import io.vertx.mcp.jrpc.model.JsonRpcError;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonRpcErrorTest {

  @Test
  public void testCreateError() {
    JsonRpcError error = new JsonRpcError(-32700, "Parse error", "Invalid JSON");
    assertEquals(-32700, error.getCode());
    assertEquals("Parse error", error.getMessage());
    assertEquals("Invalid JSON", error.getData());
  }

  @Test
  public void testCreateErrorWithoutData() {
    JsonRpcError error = new JsonRpcError(-32700, "Parse error");
    assertEquals(-32700, error.getCode());
    assertEquals("Parse error", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testParseError() {
    JsonRpcError error = JsonRpcError.parseError();
    assertEquals(-32700, error.getCode());
    assertEquals("Parse error", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testParseErrorWithDetails() {
    JsonRpcError error = JsonRpcError.parseError("Invalid JSON");
    assertEquals(-32700, error.getCode());
    assertEquals("Parse error", error.getMessage());
    assertEquals("Invalid JSON", error.getData());
  }

  @Test
  public void testInvalidRequest() {
    JsonRpcError error = JsonRpcError.invalidRequest();
    assertEquals(-32600, error.getCode());
    assertEquals("Invalid Request", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testInvalidRequestWithDetails() {
    JsonRpcError error = JsonRpcError.invalidRequest("Missing method");
    assertEquals(-32600, error.getCode());
    assertEquals("Invalid Request", error.getMessage());
    assertEquals("Missing method", error.getData());
  }

  @Test
  public void testMethodNotFound() {
    JsonRpcError error = JsonRpcError.methodNotFound();
    assertEquals(-32601, error.getCode());
    assertEquals("Method not found", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testMethodNotFoundWithMethod() {
    JsonRpcError error = JsonRpcError.methodNotFound("subtract");
    assertEquals(-32601, error.getCode());
    assertEquals("Method not found", error.getMessage());
    assertEquals("subtract", error.getData());
  }

  @Test
  public void testInvalidParams() {
    JsonRpcError error = JsonRpcError.invalidParams();
    assertEquals(-32602, error.getCode());
    assertEquals("Invalid params", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testInvalidParamsWithDetails() {
    JsonRpcError error = JsonRpcError.invalidParams("Expected array, got object");
    assertEquals(-32602, error.getCode());
    assertEquals("Invalid params", error.getMessage());
    assertEquals("Expected array, got object", error.getData());
  }

  @Test
  public void testInternalError() {
    JsonRpcError error = JsonRpcError.internalError();
    assertEquals(-32603, error.getCode());
    assertEquals("Internal error", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testInternalErrorWithDetails() {
    JsonRpcError error = JsonRpcError.internalError("Database connection failed");
    assertEquals(-32603, error.getCode());
    assertEquals("Internal error", error.getMessage());
    assertEquals("Database connection failed", error.getData());
  }

  @Test
  public void testServerError() {
    JsonRpcError error = JsonRpcError.serverError(-32000, "Custom server error");
    assertEquals(-32000, error.getCode());
    assertEquals("Custom server error", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testServerErrorWithData() {
    JsonRpcError error = JsonRpcError.serverError(-32000, "Custom server error", "Additional info");
    assertEquals(-32000, error.getCode());
    assertEquals("Custom server error", error.getMessage());
    assertEquals("Additional info", error.getData());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testServerErrorInvalidCodeTooLow() {
    JsonRpcError.serverError(-32100, "Invalid code");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testServerErrorInvalidCodeTooHigh() {
    JsonRpcError.serverError(-31999, "Invalid code");
  }

  @Test
  public void testFromJson() {
    JsonObject json = new JsonObject()
      .put("code", -32700)
      .put("message", "Parse error")
      .put("data", "Invalid JSON");

    JsonRpcError error = JsonRpcError.fromJson(json);
    assertEquals(-32700, error.getCode());
    assertEquals("Parse error", error.getMessage());
    assertEquals("Invalid JSON", error.getData());
  }

  @Test
  public void testFromJsonWithoutData() {
    JsonObject json = new JsonObject()
      .put("code", -32700)
      .put("message", "Parse error");

    JsonRpcError error = JsonRpcError.fromJson(json);
    assertEquals(-32700, error.getCode());
    assertEquals("Parse error", error.getMessage());
    assertNull(error.getData());
  }

  @Test
  public void testToJson() {
    JsonRpcError error = new JsonRpcError(-32700, "Parse error", "Invalid JSON");
    JsonObject json = error.toJson();
    assertEquals(-32700, json.getInteger("code").intValue());
    assertEquals("Parse error", json.getString("message"));
    assertEquals("Invalid JSON", json.getString("data"));
  }

  @Test
  public void testToJsonWithoutData() {
    JsonRpcError error = new JsonRpcError(-32700, "Parse error");
    JsonObject json = error.toJson();
    assertEquals(-32700, json.getInteger("code").intValue());
    assertEquals("Parse error", json.getString("message"));
    assertFalse(json.containsKey("data"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonMissingCode() {
    JsonObject json = new JsonObject()
      .put("message", "Parse error");

    JsonRpcError.fromJson(json);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonMissingMessage() {
    JsonObject json = new JsonObject()
      .put("code", -32700);

    JsonRpcError.fromJson(json);
  }
}
