package io.vertx.jrpc.mcp;

import io.vertx.core.Future;

import java.net.URI;
import java.util.function.Function;

public interface ModelContextProtocolResourceProvider extends Function<URI, Future<ModelContextProtocolResource>> {

}
