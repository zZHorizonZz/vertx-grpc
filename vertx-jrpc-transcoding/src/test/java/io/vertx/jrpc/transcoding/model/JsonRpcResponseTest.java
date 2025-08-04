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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the JsonRpcResponse class.
 */
public class JsonRpcResponseTest {

  /*@Test
  public void testCreateSuccessResponse() {
    JsonRpcResponse response = new JsonRpcResponse(19, 1);
    assertEquals("2.0", response.getJsonrpc());
    assertEquals(19, response.getResult());
    assertNull(response.getError());
    assertEquals(1, response.getId());
    assertTrue(response.isSuccess());
  }

  @Test
  public void testCreateErrorResponse() {
    JsonRpcError error = JsonRpcError.methodNotFound("subtract");
    JsonRpcResponse response = new JsonRpcResponse(error, 1);
    assertEquals("2.0", response.getJsonrpc());
    assertNull(response.getResult());
    assertEquals(error, response.getError());
    assertEquals(1, response.getId());
    assertFalse(response.isSuccess());
  }

  @Test
  public void testSuccessFromRequest() {
    JsonRpcRequest request = new JsonRpcRequest("subtract", new JsonArray().add(42).add(23), 1);
    JsonRpcResponse response = JsonRpcResponse.success(request, 19);
    assertEquals("2.0", response.getJsonrpc());
    assertEquals(19, response.getResult());
    assertNull(response.getError());
    assertEquals(1, response.getId());
    assertTrue(response.isSuccess());
  }

  @Test
  public void testErrorFromRequest() {
    JsonRpcRequest request = new JsonRpcRequest("subtract", new JsonArray().add(42).add(23), 1);
    JsonRpcError error = JsonRpcError.methodNotFound("subtract");
    JsonRpcResponse response = JsonRpcResponse.error(request, error);
    assertEquals("2.0", response.getJsonrpc());
    assertNull(response.getResult());
    assertEquals(error, response.getError());
    assertEquals(1, response.getId());
    assertFalse(response.isSuccess());
  }

  @Test
  public void testErrorFromNullRequest() {
    JsonRpcError error = JsonRpcError.parseError();
    JsonRpcResponse response = JsonRpcResponse.error(null, error);
    assertEquals("2.0", response.getJsonrpc());
    assertNull(response.getResult());
    assertEquals(error, response.getError());
    assertNull(response.getId());
    assertFalse(response.isSuccess());
  }

  @Test
  public void testFromJsonSuccess() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("result", 19)
      .put("id", 1);

    JsonRpcResponse response = JsonRpcResponse.fromJson(json);
    assertEquals("2.0", response.getJsonrpc());
    assertEquals(19, response.getResult());
    assertNull(response.getError());
    assertEquals(1, response.getId());
    assertTrue(response.isSuccess());
  }

  @Test
  public void testFromJsonError() {
    JsonObject errorObj = new JsonObject()
      .put("code", -32601)
      .put("message", "Method not found")
      .put("data", "subtract");

    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("error", errorObj)
      .put("id", 1);

    JsonRpcResponse response = JsonRpcResponse.fromJson(json);
    assertEquals("2.0", response.getJsonrpc());
    assertNull(response.getResult());
    JsonRpcError error = response.getError();
    assertNotNull(error);
    assertEquals(-32601, error.getCode());
    assertEquals("Method not found", error.getMessage());
    assertEquals("subtract", error.getData());
    assertEquals(1, response.getId());
    assertFalse(response.isSuccess());
  }

  @Test
  public void testToJsonSuccess() {
    JsonRpcResponse response = new JsonRpcResponse(19, 1);
    JsonObject json = response.toJson();
    assertEquals("2.0", json.getString("jsonrpc"));
    assertEquals(19, json.getInteger("result").intValue());
    assertFalse(json.containsKey("error"));
    assertEquals(1, json.getInteger("id").intValue());
  }

  @Test
  public void testToJsonError() {
    JsonRpcError error = JsonRpcError.methodNotFound("subtract");
    JsonRpcResponse response = new JsonRpcResponse(error, 1);
    JsonObject json = response.toJson();
    assertEquals("2.0", json.getString("jsonrpc"));
    assertFalse(json.containsKey("result"));
    JsonObject errorObj = json.getJsonObject("error");
    assertNotNull(errorObj);
    assertEquals(-32601, errorObj.getInteger("code").intValue());
    assertEquals("Method not found", errorObj.getString("message"));
    assertEquals("subtract", errorObj.getString("data"));
    assertEquals(1, json.getInteger("id").intValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonInvalidVersion() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "1.0")
      .put("result", 19)
      .put("id", 1);

    JsonRpcResponse.fromJson(json);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonMissingId() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("result", 19);

    JsonRpcResponse.fromJson(json);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonBothResultAndError() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("result", 19)
      .put("error", new JsonObject().put("code", -32601).put("message", "Method not found"))
      .put("id", 1);

    JsonRpcResponse.fromJson(json);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonNeitherResultNorError() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("id", 1);

    JsonRpcResponse.fromJson(json);
  }*/
}
