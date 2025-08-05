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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a JSON-RPC 2.0 batch request or response.
 * <p>
 * A batch allows sending multiple requests or responses in a single JSON-RPC call.
 */
public class JsonRpcBatch {

  private final List<Object> items;
  private final boolean isRequest;

  /**
   * Creates a new JSON-RPC batch.
   *
   * @param items the batch items (requests or responses)
   * @param isRequest true if this is a batch of requests, false if it's a batch of responses
   */
  private JsonRpcBatch(List<Object> items, boolean isRequest) {
    this.items = List.copyOf(items);
    this.isRequest = isRequest;
  }

  /**
   * Creates a new JSON-RPC batch request.
   *
   * @param requests the list of requests
   * @return a new batch request
   */
  public static JsonRpcBatch requestBatch(List<JsonRpcRequest> requests) {
    return new JsonRpcBatch(new ArrayList<>(requests), true);
  }

  /**
   * Creates a new JSON-RPC batch response.
   *
   * @param responses the list of responses
   * @return a new batch response
   */
  public static JsonRpcBatch responseBatch(List<JsonRpcResponse> responses) {
    return new JsonRpcBatch(new ArrayList<>(responses), false);
  }

  /**
   * Creates a JSON-RPC batch from a JsonArray.
   *
   * @param array the JsonArray representing the batch
   * @param isRequest true if this is a batch of requests, false if it's a batch of responses
   * @return a new JsonRpcBatch
   * @throws IllegalArgumentException if the JsonArray is not a valid JSON-RPC batch
   */
  public static JsonRpcBatch fromJson(JsonArray array, boolean isRequest) {
    if (array.isEmpty()) {
      throw new IllegalArgumentException("Batch cannot be empty");
    }

    List<Object> items = new ArrayList<>();
    for (Object item : array) {
      if (!(item instanceof JsonObject)) {
        throw new IllegalArgumentException("Batch items must be objects");
      }

      JsonObject jsonObject = (JsonObject) item;
      if (isRequest) {
        try {
          items.add(JsonRpcRequest.fromJson(jsonObject));
        } catch (IllegalArgumentException e) {
          // For invalid requests in a batch, we include a special marker
          // that will be converted to an error response
          items.add(new InvalidRequestMarker(jsonObject, e.getMessage()));
        }
      } else {
        items.add(JsonRpcResponse.fromJson(jsonObject));
      }
    }

    return new JsonRpcBatch(items, isRequest);
  }

  /**
   * Converts this batch to a JsonArray.
   *
   * @return the JsonArray representation of this batch
   */
  public JsonArray toJson() {
    JsonArray array = new JsonArray();
    for (Object item : items) {
      if (item instanceof JsonRpcRequest) {
        array.add(((JsonRpcRequest) item).toJson());
      } else if (item instanceof JsonRpcResponse) {
        array.add(((JsonRpcResponse) item).toJson());
      } else if (item instanceof InvalidRequestMarker) {
        // Skip invalid request markers when serializing a request batch
        if (!isRequest) {
          // This shouldn't happen in normal operation
          throw new IllegalStateException("Invalid request marker found in response batch");
        }
      } else {
        throw new IllegalStateException("Unknown batch item type: " + item.getClass().getName());
      }
    }
    return array;
  }

  /**
   * @return the batch items (requests or responses)
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getItems() {
    return (List<T>) items;
  }

  /**
   * @return true if this is a batch of requests, false if it's a batch of responses
   */
  public boolean isRequest() {
    return isRequest;
  }

  /**
   * @return true if this is a batch of responses, false if it's a batch of requests
   */
  public boolean isResponse() {
    return !isRequest;
  }

  /**
   * Creates a response batch for this request batch.
   * <p>
   * This method processes each request in the batch and generates a corresponding response.
   * Notifications (requests without an id) do not generate responses.
   * Invalid requests generate error responses.
   *
   * @param processor the function that processes each request and returns a response
   * @return a new batch response
   * @throws IllegalStateException if this is not a batch of requests
   */
  public JsonRpcBatch processRequests(RequestProcessor processor) {
    if (!isRequest) {
      throw new IllegalStateException("Cannot process responses as requests");
    }

    List<JsonRpcResponse> responses = new ArrayList<>();
    for (Object item : items) {
      if (item instanceof JsonRpcRequest) {
        JsonRpcRequest request = (JsonRpcRequest) item;
        // Skip notifications (requests without an id)
        if (!request.isNotification()) {
          JsonRpcResponse response = processor.process(request);
          responses.add(response);
        }
      } else if (item instanceof InvalidRequestMarker) {
        InvalidRequestMarker marker = (InvalidRequestMarker) item;
        // Generate an error response for invalid requests
        JsonRpcResponse response = new JsonRpcResponse(
          JsonRpcError.invalidRequest(marker.errorMessage),
          null
        );
        responses.add(response);
      }
    }

    return responseBatch(responses);
  }

  /**
   * Functional interface for processing JSON-RPC requests.
   */
  @FunctionalInterface
  public interface RequestProcessor {
    /**
     * Processes a JSON-RPC request and returns a response.
     *
     * @param request the request to process
     * @return the response
     */
    JsonRpcResponse process(JsonRpcRequest request);
  }

  /**
   * Marker class for invalid requests in a batch.
   * <p>
   * This is used to track invalid requests so that appropriate error responses can be generated.
   */
  private static class InvalidRequestMarker {
    private final JsonObject originalJson;
    private final String errorMessage;

    InvalidRequestMarker(JsonObject originalJson, String errorMessage) {
      this.originalJson = originalJson;
      this.errorMessage = errorMessage;
    }
  }
}
