# Vert.x JSON-RPC Transcoding

This module provides JSON-RPC transcoding support for Vert.x gRPC, allowing you to expose your gRPC services as JSON-RPC endpoints.

## Overview

JSON-RPC is a stateless, light-weight remote procedure call (RPC) protocol that uses JSON for data format. This module allows you to:

- Expose your gRPC services as JSON-RPC endpoints
- Handle both positional and named parameters
- Support notifications (requests without responses)
- Support batch requests and responses
- Handle errors according to the JSON-RPC 2.0 specification

## Usage

### Maven Dependency

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-jrpc-transcoding</artifactId>
  <version>${vertx.version}</version>
</dependency>
```

### Basic Usage

The JSON-RPC transcoding module is automatically registered with the Vert.x gRPC server through the Java Service Provider Interface (SPI). When you create a gRPC server, it will automatically detect and use the JSON-RPC transcoding module.

```java
// Create a gRPC server
GrpcServer server = GrpcServer.server(vertx);

// Register your service
server.callHandler(myService);

// Start the server
server.listen(8080);
```

### JSON-RPC Requests

JSON-RPC requests should be sent as HTTP POST requests with the `Content-Type` header set to `application/json`. The request body should be a valid JSON-RPC 2.0 request object:

```json
{
  "jsonrpc": "2.0",
  "method": "myMethod",
  "params": [1, 2, 3],
  "id": 1
}
```

or with named parameters:

```json
{
  "jsonrpc": "2.0",
  "method": "myMethod",
  "params": {"param1": 1, "param2": 2, "param3": 3},
  "id": 1
}
```

### JSON-RPC Responses

The server will respond with a JSON-RPC 2.0 response object:

```json
{
  "jsonrpc": "2.0",
  "result": 42,
  "id": 1
}
```

or in case of an error:

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": "myMethod"
  },
  "id": 1
}
```

### Notifications

JSON-RPC notifications are requests without an `id` field. The server will not send a response for notifications:

```json
{
  "jsonrpc": "2.0",
  "method": "myMethod",
  "params": [1, 2, 3]
}
```

### Batch Requests

JSON-RPC batch requests allow you to send multiple requests in a single HTTP request:

```json
[
  {"jsonrpc": "2.0", "method": "method1", "params": [1, 2], "id": 1},
  {"jsonrpc": "2.0", "method": "method2", "params": {"a": 1, "b": 2}, "id": 2},
  {"jsonrpc": "2.0", "method": "notify", "params": [1, 2, 3]}
]
```

The server will respond with a batch response containing responses for all non-notification requests:

```json
[
  {"jsonrpc": "2.0", "result": 3, "id": 1},
  {"jsonrpc": "2.0", "result": 3, "id": 2}
]
```

## Error Handling

The JSON-RPC transcoding module follows the JSON-RPC 2.0 specification for error handling. The following error codes are used:

- `-32700`: Parse error - Invalid JSON was received
- `-32600`: Invalid Request - The JSON sent is not a valid Request object
- `-32601`: Method not found - The method does not exist / is not available
- `-32602`: Invalid params - Invalid method parameter(s)
- `-32603`: Internal error - Internal JSON-RPC error
- `-32000` to `-32099`: Server error - Reserved for implementation-defined server-errors

## Limitations

- The current implementation does not support streaming methods
**- Batch requests are processed sequentially, not in parallel**

## References

- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
