package io.vertx.jrpc.mcp;

import io.vertx.grpc.server.Service;

/**
 * The ModelContextProtocolService provides methods for working with MCP (Model Context Protocol). This interface extends the base Service interface and is implemented by
 * ModelContextProtocolServiceImpl.
 */
public interface ModelContextProtocolService extends Service {

  void addTool(ModelContextProtocolTool tool);

  void addResource(ModelContextProtocolResource resource);

  void addPrompt(ModelContextProtocolPrompt prompt);

}
