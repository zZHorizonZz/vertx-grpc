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

import com.google.protobuf.Timestamp;
import io.vertx.core.json.JsonObject;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.core.Schemas;
import io.vertx.schema.protobuf.Proto;
import io.vertx.schema.protobuf.ProtoWire;
import io.vertx.tests.schema.proto.Color;
import io.vertx.tests.schema.proto.Sample;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

/**
 * Proves {@link ProtoWire}'s ENUM, {@code map<string,V>} and TIMESTAMP support is wire-compatible
 * with stock protobuf, by round-tripping against the generated {@code Sample} message both ways.
 */
public class ProtoTypesTest {

  static final ObjectSchema SAMPLE = Schemas.object()
    .field("name", Proto.field(1, Schemas.string()))
    .field("color", Proto.field(2, Schemas.enumOf("RED", "GREEN", "BLUE")))
    .field("labels", Proto.field(3, Schemas.record(Schemas.string())))
    .field("counts", Proto.field(4, Schemas.record(Schemas.int32())))
    .field("created", Proto.field(5, Schemas.timestamp()));

  static final Instant CREATED = Instant.ofEpochSecond(1_700_000_000L, 123_456_789);

  @Test
  public void schemaWriteDecodesWithProtobuf() throws Exception {
    JsonObject value = new JsonObject()
      .put("name", "widget")
      .put("color", "GREEN")
      .put("labels", new JsonObject().put("env", "prod").put("tier", "gold"))
      .put("counts", new JsonObject().put("a", 1).put("b", 2))
      .put("created", CREATED);

    Sample sample = Sample.parseFrom(ProtoWire.write(SAMPLE, value));

    assertEquals("widget", sample.getName());
    assertEquals(Color.GREEN, sample.getColor());
    assertEquals("prod", sample.getLabelsMap().get("env"));
    assertEquals("gold", sample.getLabelsMap().get("tier"));
    assertEquals(Integer.valueOf(1), sample.getCountsMap().get("a"));
    assertEquals(Integer.valueOf(2), sample.getCountsMap().get("b"));
    assertEquals(1_700_000_000L, sample.getCreated().getSeconds());
    assertEquals(123_456_789, sample.getCreated().getNanos());
  }

  @Test
  public void schemaReadsProtobufBytes() throws Exception {
    Sample sample = Sample.newBuilder()
      .setName("widget")
      .setColor(Color.BLUE)
      .putLabels("env", "prod")
      .putCounts("a", 7)
      .setCreated(Timestamp.newBuilder().setSeconds(1_700_000_000L).setNanos(123_456_789).build())
      .build();

    JsonObject back = ProtoWire.read(SAMPLE, sample.toByteArray());

    assertEquals("widget", back.getString("name"));
    assertEquals("BLUE", back.getString("color"));
    assertEquals("prod", back.getJsonObject("labels").getString("env"));
    assertEquals(Integer.valueOf(7), back.getJsonObject("counts").getInteger("a"));
    assertEquals(CREATED, back.getInstant("created"));
  }
}
