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
package io.vertx.schema.protobuf;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.schema.FieldType;
import io.vertx.schema.Schema;
import io.vertx.schema.core.ArraySchema;
import io.vertx.schema.core.EnumSchema;
import io.vertx.schema.core.ObjectSchema;
import io.vertx.schema.core.RecordSchema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Protobuf wire target for a general {@link ObjectSchema}: it turns a schema plus a
 * {@link JsonObject} value into Protobuf-compatible bytes and back, without any generated
 * message classes. Field numbers are read from each field's metadata channel ({@link Proto#FIELD}).
 * <p>
 * The bytes it produces are byte-for-byte identical to what {@code protobuf-java} produces for
 * the equivalent message, so a schema-defined service is wire interoperable with regular
 * Protobuf/gRPC peers.
 * <p>
 * Supported: STRING, INT32, INT64, BOOL, BYTES, FLOAT32, FLOAT64, nested OBJECT, ENUM (varint of
 * the value's declaration index), TIMESTAMP (as {@code google.protobuf.Timestamp}), {@code record}
 * maps (as protobuf {@code map<string,V>}), and (unpacked) repeated ARRAY of scalars/messages.
 * proto3 default values are omitted on the wire. TODO: UUID, packed repeated scalars, custom enum
 * numbers, and zig-zag/fixed encodings (via a Proto.ENCODING metadata key).
 */
public final class ProtoWire {

  private ProtoWire() {
  }

  public static byte[] write(ObjectSchema schema, JsonObject message) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CodedOutputStream out = CodedOutputStream.newInstance(baos);
    try {
      writeObject(schema, message, out);
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return baos.toByteArray();
  }

  public static JsonObject read(ObjectSchema schema, byte[] bytes) {
    try {
      return readObject(schema, CodedInputStream.newInstance(bytes));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeObject(ObjectSchema schema, JsonObject message, CodedOutputStream out) throws IOException {
    for (Map.Entry<String, Schema<?, ?>> entry : schema.fields().entrySet()) {
      String name = entry.getKey();
      Schema<?, ?> field = entry.getValue();
      Object value = message.getValue(name);
      if (value == null) {
        continue;
      }
      int number = fieldNumber(name, field);
      if (field instanceof ArraySchema) {
        Schema<?, ?> element = ((ArraySchema<?>) field).element();
        JsonArray array = (JsonArray) value;
        for (int i = 0; i < array.size(); i++) {
          writeValue(element, number, array.getValue(i), out);
        }
      } else if (field instanceof RecordSchema) {
        writeMap(number, (RecordSchema<?>) field, (JsonObject) value, out);
      } else if (!isDefault(field, value)) {
        writeValue(field, number, value, out);
      }
    }
  }

  private static void writeValue(Schema<?, ?> schema, int number, Object value, CodedOutputStream out) throws IOException {
    if (schema instanceof EnumSchema) {
      out.writeInt32(number, enumIndex((EnumSchema<?>) schema, value));
      return;
    }
    if (schema instanceof ObjectSchema) {
      // An embedded message is encoded exactly like a length-delimited bytes field.
      out.writeByteArray(number, write((ObjectSchema) schema, (JsonObject) value));
      return;
    }
    FieldType<?> type = schema.type();
    if (type == FieldType.STRING) {
      out.writeString(number, (String) value);
    } else if (type == FieldType.INT32) {
      out.writeInt32(number, ((Number) value).intValue());
    } else if (type == FieldType.INT64) {
      out.writeInt64(number, ((Number) value).longValue());
    } else if (type == FieldType.BOOL) {
      out.writeBool(number, (Boolean) value);
    } else if (type == FieldType.BYTES) {
      out.writeByteArray(number, (byte[]) value);
    } else if (type == FieldType.FLOAT32) {
      out.writeFloat(number, ((Number) value).floatValue());
    } else if (type == FieldType.FLOAT64) {
      out.writeDouble(number, ((Number) value).doubleValue());
    } else if (type == FieldType.TIMESTAMP) {
      out.writeByteArray(number, writeTimestamp(toInstant(value)));
    } else {
      throw new IllegalStateException("Unsupported field type for protobuf wire: " + type.typeName());
    }
  }

  private static void writeMap(int number, RecordSchema<?> record, JsonObject map, CodedOutputStream out) throws IOException {
    Schema<?, ?> valueSchema = record.valueSchema();
    for (String key : map.fieldNames()) {
      Object value = map.getValue(key);
      // A protobuf map entry is a synthetic message: field 1 = key, field 2 = value.
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream entry = CodedOutputStream.newInstance(baos);
      if (key != null && !key.isEmpty()) {
        entry.writeString(1, key);
      }
      if (value != null && !isDefault(valueSchema, value)) {
        writeValue(valueSchema, 2, value, entry);
      }
      entry.flush();
      out.writeByteArray(number, baos.toByteArray());
    }
  }

  private static Instant toInstant(Object value) {
    // Vert.x JsonObject stores an Instant as an ISO-8601 string, so accept either form.
    return value instanceof Instant ? (Instant) value : Instant.parse(value.toString());
  }

  private static byte[] writeTimestamp(Instant ts) throws IOException {
    // google.protobuf.Timestamp { int64 seconds = 1; int32 nanos = 2; }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CodedOutputStream out = CodedOutputStream.newInstance(baos);
    long seconds = ts.getEpochSecond();
    int nanos = ts.getNano();
    if (seconds != 0L) {
      out.writeInt64(1, seconds);
    }
    if (nanos != 0) {
      out.writeInt32(2, nanos);
    }
    out.flush();
    return baos.toByteArray();
  }

  private static JsonObject readObject(ObjectSchema schema, CodedInputStream in) throws IOException {
    Map<Integer, Map.Entry<String, Schema<?, ?>>> byNumber = new HashMap<>();
    for (Map.Entry<String, Schema<?, ?>> entry : schema.fields().entrySet()) {
      byNumber.put(fieldNumber(entry.getKey(), entry.getValue()), entry);
    }
    JsonObject result = new JsonObject();
    while (true) {
      int tag = in.readTag();
      if (tag == 0) {
        break;
      }
      Map.Entry<String, Schema<?, ?>> entry = byNumber.get(tag >>> 3);
      if (entry == null) {
        in.skipField(tag);
        continue;
      }
      String name = entry.getKey();
      Schema<?, ?> field = entry.getValue();
      if (field instanceof ArraySchema) {
        JsonArray array = (JsonArray) result.getValue(name);
        if (array == null) {
          array = new JsonArray();
          result.put(name, array);
        }
        array.add(readValue(((ArraySchema<?>) field).element(), in));
      } else if (field instanceof RecordSchema) {
        JsonObject map = (JsonObject) result.getValue(name);
        if (map == null) {
          map = new JsonObject();
          result.put(name, map);
        }
        readMapEntry((RecordSchema<?>) field, in.readByteArray(), map);
      } else {
        result.put(name, readValue(field, in));
      }
    }
    return result;
  }

  private static Object readValue(Schema<?, ?> schema, CodedInputStream in) throws IOException {
    if (schema instanceof EnumSchema) {
      int index = in.readInt32();
      List<?> values = ((EnumSchema<?>) schema).values();
      return index >= 0 && index < values.size() ? values.get(index) : index;
    }
    if (schema instanceof ObjectSchema) {
      return read((ObjectSchema) schema, in.readByteArray());
    }
    FieldType<?> type = schema.type();
    if (type == FieldType.STRING) {
      return in.readString();
    } else if (type == FieldType.INT32) {
      return in.readInt32();
    } else if (type == FieldType.INT64) {
      return in.readInt64();
    } else if (type == FieldType.BOOL) {
      return in.readBool();
    } else if (type == FieldType.BYTES) {
      return in.readByteArray();
    } else if (type == FieldType.FLOAT32) {
      return in.readFloat();
    } else if (type == FieldType.FLOAT64) {
      return in.readDouble();
    } else if (type == FieldType.TIMESTAMP) {
      return readTimestamp(in.readByteArray());
    } else {
      throw new IllegalStateException("Unsupported field type for protobuf wire: " + type.typeName());
    }
  }

  private static void readMapEntry(RecordSchema<?> record, byte[] bytes, JsonObject map) throws IOException {
    Schema<?, ?> valueSchema = record.valueSchema();
    CodedInputStream in = CodedInputStream.newInstance(bytes);
    String key = "";
    Object value = null;
    while (true) {
      int tag = in.readTag();
      if (tag == 0) {
        break;
      }
      int number = tag >>> 3;
      if (number == 1) {
        key = in.readString();
      } else if (number == 2) {
        value = readValue(valueSchema, in);
      } else {
        in.skipField(tag);
      }
    }
    map.put(key, value != null ? value : defaultValue(valueSchema));
  }

  private static Instant readTimestamp(byte[] bytes) throws IOException {
    CodedInputStream in = CodedInputStream.newInstance(bytes);
    long seconds = 0L;
    int nanos = 0;
    while (true) {
      int tag = in.readTag();
      if (tag == 0) {
        break;
      }
      int number = tag >>> 3;
      if (number == 1) {
        seconds = in.readInt64();
      } else if (number == 2) {
        nanos = in.readInt32();
      } else {
        in.skipField(tag);
      }
    }
    return Instant.ofEpochSecond(seconds, nanos);
  }

  private static int enumIndex(EnumSchema<?> schema, Object value) {
    int index = schema.values().indexOf(value);
    if (index < 0) {
      throw new IllegalArgumentException("Value '" + value + "' is not one of the enum's declared values");
    }
    return index;
  }

  private static int fieldNumber(String name, Schema<?, ?> field) {
    int number = field.metaOr(Proto.FIELD, 0);
    if (number <= 0) {
      throw new IllegalStateException("Field '" + name + "' is missing a protobuf field number (.meta(Proto.FIELD, n))");
    }
    return number;
  }

  private static boolean isDefault(Schema<?, ?> schema, Object value) {
    if (schema instanceof EnumSchema) {
      return enumIndex((EnumSchema<?>) schema, value) == 0;
    }
    FieldType<?> type = schema.type();
    if (type == FieldType.INT32) {
      return ((Number) value).intValue() == 0;
    } else if (type == FieldType.INT64) {
      return ((Number) value).longValue() == 0L;
    } else if (type == FieldType.BOOL) {
      return !((Boolean) value);
    } else if (type == FieldType.STRING) {
      return ((String) value).isEmpty();
    } else if (type == FieldType.BYTES) {
      return ((byte[]) value).length == 0;
    } else if (type == FieldType.FLOAT32) {
      return ((Number) value).floatValue() == 0f;
    } else if (type == FieldType.FLOAT64) {
      return ((Number) value).doubleValue() == 0d;
    }
    return false;
  }

  private static Object defaultValue(Schema<?, ?> schema) {
    if (schema instanceof EnumSchema) {
      return ((EnumSchema<?>) schema).values().get(0);
    }
    if (schema instanceof ObjectSchema) {
      return new JsonObject();
    }
    FieldType<?> type = schema.type();
    if (type == FieldType.INT32) {
      return 0;
    } else if (type == FieldType.INT64) {
      return 0L;
    } else if (type == FieldType.BOOL) {
      return Boolean.FALSE;
    } else if (type == FieldType.STRING) {
      return "";
    } else if (type == FieldType.BYTES) {
      return new byte[0];
    } else if (type == FieldType.FLOAT32) {
      return 0f;
    } else if (type == FieldType.FLOAT64) {
      return 0d;
    } else if (type == FieldType.TIMESTAMP) {
      return Instant.EPOCH;
    }
    return null;
  }
}
