package io.vertx.mcp;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ModelContextProtocolOptions {

  public static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";

  public static final String DEFAULT_SERVER_VERSION = "1.0";

  public static final String DEFAULT_SERVER_NAME = "Vert.x MCP Server";

  private String protocolVersion;
  private String serverVersion;
  private String serverName;

  public ModelContextProtocolOptions() {
    this.protocolVersion = DEFAULT_PROTOCOL_VERSION;
    this.serverVersion = DEFAULT_SERVER_VERSION;
    this.serverName = DEFAULT_SERVER_NAME;
  }

  public ModelContextProtocolOptions(ModelContextProtocolOptions other) {
    this.protocolVersion = other.protocolVersion;
    this.serverVersion = other.serverVersion;
    this.serverName = other.serverName;
  }

  public ModelContextProtocolOptions(JsonObject json) {
    this();
    ModelContextProtocolOptionsConverter.fromJson(json, this);
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public ModelContextProtocolOptions setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
    return this;
  }

  public String getServerVersion() {
    return serverVersion;
  }

  public ModelContextProtocolOptions setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
    return this;
  }

  public String getServerName() {
    return serverName;
  }

  public ModelContextProtocolOptions setServerName(String serverName) {
    this.serverName = serverName;
    return this;
  }

  /**
   * @return a JSON representation of options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ModelContextProtocolOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
