package com.simba.snip.npo.mcp;

import java.util.Map;

public interface McpTool {

    String name();

    String description();

    Map<String, Object> call(Map<String, Object> arguments);
}
