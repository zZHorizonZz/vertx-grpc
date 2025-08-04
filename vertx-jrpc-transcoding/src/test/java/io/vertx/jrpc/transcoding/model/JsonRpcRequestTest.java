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
 * Tests for the JsonRpcRequest class.
 */
public class JsonRpcRequestTest {

  @Test
  public void testCreateRequest() {
    JsonRpcRequest request = new JsonRpcRequest("subtract", new JsonArray().add(42).add(23), "1");
    assertEquals("2.0", request.getJsonrpc());
    assertEquals("subtract", request.getMethod());
    assertEquals(new JsonArray().add(42).add(23), request.getParams());
    assertEquals("1", request.getId());
    assertFalse(request.isNotification());
  }

  @Test
  public void testCreateNotification() {
    JsonRpcRequest notification = JsonRpcRequest.createNotification("update", new JsonArray().add(1).add(2).add(3));
    assertEquals("2.0", notification.getJsonrpc());
    assertEquals("update", notification.getMethod());
    assertEquals(new JsonArray().add(1).add(2).add(3), notification.getParams());
    assertNull(notification.getId());
    assertTrue(notification.isNotification());
  }

  @Test
  public void testFromJson() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "subtract")
      .put("params", new JsonArray().add(42).add(23))
      .put("id", 1);

    JsonRpcRequest request = JsonRpcRequest.fromJson(json);
    assertEquals("2.0", request.getJsonrpc());
    assertEquals("subtract", request.getMethod());
    assertEquals(new JsonArray().add(42).add(23), request.getParams());
    assertEquals("1", request.getId());
  }

  @Test
  public void testFromJsonWithNamedParams() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "subtract")
      .put("params", new JsonObject().put("subtrahend", 23).put("minuend", 42))
      .put("id", 3);

    JsonRpcRequest request = JsonRpcRequest.fromJson(json);
    assertEquals("2.0", request.getJsonrpc());
    assertEquals("subtract", request.getMethod());
    JsonObject params = (JsonObject) request.getParams();
    assertEquals(23, params.getInteger("subtrahend").intValue());
    assertEquals(42, params.getInteger("minuend").intValue());
    assertEquals("3", request.getId());
  }

  @Test
  public void testToJson() {
    JsonRpcRequest request = new JsonRpcRequest("subtract", new JsonArray().add(42).add(23), "1");
    JsonObject json = request.toJson();
    assertEquals("2.0", json.getString("jsonrpc"));
    assertEquals("subtract", json.getString("method"));
    assertEquals(new JsonArray().add(42).add(23), json.getJsonArray("params"));
    assertEquals("1", json.getString("id"));
  }

  @Test
  public void testToJsonNotification() {
    JsonRpcRequest notification = JsonRpcRequest.createNotification("update", new JsonArray().add(1).add(2).add(3));
    JsonObject json = notification.toJson();
    assertEquals("2.0", json.getString("jsonrpc"));
    assertEquals("update", json.getString("method"));
    assertEquals(new JsonArray().add(1).add(2).add(3), json.getJsonArray("params"));
    assertFalse(json.containsKey("id"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonInvalidVersion() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "1.0")
      .put("method", "subtract")
      .put("params", new JsonArray().add(42).add(23))
      .put("id", 1);

    JsonRpcRequest.fromJson(json);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonMissingMethod() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("params", new JsonArray().add(42).add(23))
      .put("id", 1);

    JsonRpcRequest.fromJson(json);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJsonInvalidParams() {
    JsonObject json = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "subtract")
      .put("params", "invalid")
      .put("id", 1);

    JsonRpcRequest.fromJson(json);
  }
}
