### MCP Config

```json
{
  "mcpServers": {
    "weather": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://localhost:8080/io.modelcontextprotocol.ModelContextProtocolService"
      ]
    }
  }
}
```

### Debugging

For debugging of MCP we can launch [mcp inspector](https://github.com/modelcontextprotocol/inspector)
```shell
npx @modelcontextprotocol/inspector
```
