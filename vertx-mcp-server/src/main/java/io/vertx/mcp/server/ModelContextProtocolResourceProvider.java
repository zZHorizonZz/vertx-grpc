package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.ModelContextProtocolResource;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

public interface ModelContextProtocolResourceProvider extends Function<URI, Future<List<ModelContextProtocolResource>>> {

}
