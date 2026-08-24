package com.simba.snip.npo.mcp;

import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Local Java MCP server. JSON-RPC 2.0 over HTTP. Not exposed as an LLM tool handle.
 */
@RestController
public class McpServerController {

    private final Map<String, McpTool> tools;

    public McpServerController(List<McpTool> tools) {
        this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(McpTool::name, Function.identity()));
    }

    @GetMapping(path = "/mcp/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        return Map.of("status", "UP", "protocol", "mcp", "tools", tools.keySet());
    }

    @PostMapping(path = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handle(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        String method = String.valueOf(request.get("method"));
        try {
            Object result = switch (method) {
                case "initialize" -> initialize();
                case "tools/list" -> toolsList();
                case "tools/call" -> toolsCall(request.get("params"));
                default -> throw new DomainValidationException("unsupported MCP method: " + method);
            };
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.put("result", result);
            return response;
        } catch (RuntimeException ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", -32000);
            error.put("message", ex.getMessage());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.put("error", error);
            return response;
        }
    }

    private Map<String, Object> initialize() {
        return Map.of(
                "protocolVersion", "2024-11-05",
                "serverInfo", Map.of("name", "snip-npo-mcp", "version", "1"),
                "capabilities", Map.of("tools", Map.of())
        );
    }

    private Map<String, Object> toolsList() {
        List<Map<String, Object>> listed = tools.values().stream()
                .map(tool -> Map.<String, Object>of(
                        "name", tool.name(),
                        "description", tool.description()
                ))
                .toList();
        return Map.of("tools", listed);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toolsCall(Object params) {
        if (!(params instanceof Map<?, ?> raw)) {
            throw new DomainValidationException("tools/call params are required");
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        String name = String.valueOf(map.get("name"));
        McpTool tool = tools.get(name);
        if (tool == null) {
            throw new DomainValidationException("unknown MCP tool: " + name);
        }
        Map<String, Object> arguments = map.get("arguments") instanceof Map<?, ?> args
                ? (Map<String, Object>) args
                : Map.of();
        Map<String, Object> output = tool.call(arguments);
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", output)),
                "isError", false,
                "structuredContent", output
        );
    }
}
