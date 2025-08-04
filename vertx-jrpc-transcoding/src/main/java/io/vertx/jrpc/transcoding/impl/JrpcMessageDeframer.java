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
package io.vertx.jrpc.transcoding.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.MessageSizeOverflowException;
import io.vertx.grpc.common.WireFormat;
import io.vertx.grpc.common.impl.GrpcMessageDeframer;

/**
 * A message deframer for JSON-RPC messages.
 * <p>
 * Unlike gRPC, JSON-RPC doesn't use a framing protocol. The entire message is sent as a single JSON object.
 * This deframer simply passes the entire buffer as a single message.
 */
public class JrpcMessageDeframer implements GrpcMessageDeframer {

  private long maxMessageSize = Long.MAX_VALUE;
  private boolean processed;
  private Buffer buffer;
  private Object result;

  @Override
  public void maxMessageSize(long maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  @Override
  public void update(Buffer chunk) {
    if (processed) {
      return;
    }

    if (buffer == null) {
      buffer = chunk;
    } else {
      try {
        buffer.appendBuffer(chunk);
      } catch (IndexOutOfBoundsException e) {
        // Handle buffer capacity issues
        buffer = buffer.copy();
        buffer.appendBuffer(chunk);
      }
    }

    if (result == null && buffer.length() > maxMessageSize) {
      result = new MessageSizeOverflowException(buffer.length());
      buffer = null;
      processed = true;
    }
  }

  @Override
  public void end() {
    if (!processed) {
      result = GrpcMessage.message("identity", WireFormat.JSON, buffer == null ? Buffer.buffer() : buffer);
      buffer = null;
      processed = true;
    }
  }

  @Override
  public Object next() {
    if (result != null) {
      Object ret = result;
      result = null;
      return ret;
    } else {
      return null;
    }
  }
}
