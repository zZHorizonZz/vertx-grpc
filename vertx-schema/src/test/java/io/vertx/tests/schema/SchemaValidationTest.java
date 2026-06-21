/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.tests.schema;

import io.vertx.core.json.JsonObject;
import io.vertx.schema.ParseResult;
import io.vertx.schema.SchemaError;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.core.Schemas;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Smoke test proving the ported (Java 11, JsonObject-based) general schema actually validates at
 * runtime - nested objects, refinements, required fields and nested issue paths.
 */
public class SchemaValidationTest {

  static final ObjectSchema USER = Schemas.object()
    .field("name", Schemas.string().min(2))
    .field("age", Schemas.int32().min(0))
    .field("address", Schemas.object()
      .field("zip", Schemas.int32()));

  @Test
  public void validInputParses() {
    JsonObject input = new JsonObject()
      .put("name", "Ada")
      .put("age", 36)
      .put("address", new JsonObject().put("zip", 12345));

    JsonObject parsed = USER.parse(input);

    assertEquals("Ada", parsed.getString("name"));
    assertEquals(Integer.valueOf(36), parsed.getInteger("age"));
    assertEquals(Integer.valueOf(12345), parsed.getJsonObject("address").getInteger("zip"));
  }

  @Test
  public void refinementFailureIsReported() {
    JsonObject input = new JsonObject()
      .put("name", "A")
      .put("age", 36)
      .put("address", new JsonObject().put("zip", 12345));

    ParseResult<JsonObject> result = USER.safeParse(input);

    assertFalse(result.isSuccess());
    assertEquals("name", result.issues().get(0).path().render());
  }

  @Test
  public void missingRequiredNestedFieldIsReported() {
    JsonObject input = new JsonObject()
      .put("name", "Ada")
      .put("age", 36)
      .put("address", new JsonObject());

    try {
      USER.parse(input);
      fail("expected SchemaError");
    } catch (SchemaError error) {
      assertTrue(error.getMessage(), error.getMessage().contains("address"));
    }
  }
}
