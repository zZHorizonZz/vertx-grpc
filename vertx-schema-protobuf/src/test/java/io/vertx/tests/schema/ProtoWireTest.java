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

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import io.vertx.core.json.JsonObject;
import io.vertx.schema.protobuf.Proto;
import io.vertx.schema.protobuf.ProtoWire;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.core.Schemas;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Proves that {@link ProtoWire} - driven by a general, backend-agnostic schema whose fields
 * carry only a {@code pb.number} in the metadata channel - emits bytes byte-for-byte identical
 * to {@code protobuf-java}, and reads protobuf-produced bytes back. No generated message classes.
 */
public class ProtoWireTest {

  static final ObjectSchema ADDRESS = Schemas.object()
    .field("street", Proto.field(1, Schemas.string()))
    .field("zip", Proto.field(2, Schemas.int32()));

  static final ObjectSchema PERSON = Schemas.object()
    .field("name", Proto.field(1, Schemas.string()))
    .field("age", Proto.field(2, Schemas.int32()))
    .field("active", Proto.field(3, Schemas.bool()))
    .field("score", Proto.field(4, Schemas.int64()))
    .field("address", Proto.field(5, ADDRESS));

  @Test
  public void writeIsProtobufCompatible() throws Exception {
    JsonObject person = new JsonObject()
      .put("name", "Ada")
      .put("age", 36)
      .put("active", true)
      .put("score", 9_000_000_000L)
      .put("address", new JsonObject().put("street", "1 Analytical Ave").put("zip", 12345));

    byte[] ours = ProtoWire.write(PERSON, person);

    // 1) Stock protobuf-java can decode our bytes via a runtime descriptor.
    Descriptor personDesc = buildPersonDescriptor();
    Descriptor addressDesc = personDesc.findFieldByNumber(5).getMessageType();
    DynamicMessage decoded = DynamicMessage.parseFrom(personDesc, ours);
    assertEquals("Ada", decoded.getField(personDesc.findFieldByNumber(1)));
    assertEquals(36, decoded.getField(personDesc.findFieldByNumber(2)));
    assertEquals(true, decoded.getField(personDesc.findFieldByNumber(3)));
    assertEquals(9_000_000_000L, decoded.getField(personDesc.findFieldByNumber(4)));
    DynamicMessage decodedAddress = (DynamicMessage) decoded.getField(personDesc.findFieldByNumber(5));
    assertEquals("1 Analytical Ave", decodedAddress.getField(addressDesc.findFieldByNumber(1)));
    assertEquals(12345, decodedAddress.getField(addressDesc.findFieldByNumber(2)));

    // 2) Our bytes are identical to what protobuf-java itself produces.
    DynamicMessage canonical = DynamicMessage.newBuilder(personDesc)
      .setField(personDesc.findFieldByNumber(1), "Ada")
      .setField(personDesc.findFieldByNumber(2), 36)
      .setField(personDesc.findFieldByNumber(3), true)
      .setField(personDesc.findFieldByNumber(4), 9_000_000_000L)
      .setField(personDesc.findFieldByNumber(5), DynamicMessage.newBuilder(addressDesc)
        .setField(addressDesc.findFieldByNumber(1), "1 Analytical Ave")
        .setField(addressDesc.findFieldByNumber(2), 12345)
        .build())
      .build();
    assertArrayEquals(canonical.toByteArray(), ours);

    // 3) We can read protobuf-produced bytes back into the original value.
    assertEquals(person, ProtoWire.read(PERSON, canonical.toByteArray()));
  }

  private static Descriptor buildPersonDescriptor() throws Exception {
    DescriptorProto address = DescriptorProto.newBuilder()
      .setName("Address")
      .addField(scalar("street", 1, FieldDescriptorProto.Type.TYPE_STRING))
      .addField(scalar("zip", 2, FieldDescriptorProto.Type.TYPE_INT32))
      .build();
    DescriptorProto person = DescriptorProto.newBuilder()
      .setName("Person")
      .addField(scalar("name", 1, FieldDescriptorProto.Type.TYPE_STRING))
      .addField(scalar("age", 2, FieldDescriptorProto.Type.TYPE_INT32))
      .addField(scalar("active", 3, FieldDescriptorProto.Type.TYPE_BOOL))
      .addField(scalar("score", 4, FieldDescriptorProto.Type.TYPE_INT64))
      .addField(FieldDescriptorProto.newBuilder()
        .setName("address")
        .setNumber(5)
        .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
        .setTypeName(".test.Address")
        .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
        .build())
      .build();
    FileDescriptorProto file = FileDescriptorProto.newBuilder()
      .setName("test.proto")
      .setPackage("test")
      .setSyntax("proto3")
      .addMessageType(address)
      .addMessageType(person)
      .build();
    FileDescriptor fd = FileDescriptor.buildFrom(file, new FileDescriptor[0]);
    return fd.findMessageTypeByName("Person");
  }

  private static FieldDescriptorProto scalar(String name, int number, FieldDescriptorProto.Type type) {
    return FieldDescriptorProto.newBuilder()
      .setName(name)
      .setNumber(number)
      .setType(type)
      .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
      .build();
  }
}
