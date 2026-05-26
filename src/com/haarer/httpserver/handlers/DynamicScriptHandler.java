package com.haarer.httpserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DynamicScriptHandler implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(DynamicScriptHandler.class.getName());
    private final String scriptsDir;
    private final ScriptEngine engine;
    private final Map<String, ScriptCacheEntry> scriptCache = new ConcurrentHashMap<>();

    public DynamicScriptHandler(String scriptsDir) {
        this.scriptsDir = scriptsDir;
        ScriptEngineManager manager = new ScriptEngineManager(getClass().getClassLoader());
        this.engine = manager.getEngineByName("groovy");
        if (this.engine == null) {
            throw new RuntimeException("Groovy script engine not found. Please ensure Groovy is available in the JVM.");
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        // Remove leading slash and find the corresponding .groovy file
        String scriptName = path.startsWith("/") ? path.substring(1) : path;
        if (scriptName.isEmpty()) {
            scriptName = "index";
        }
        if (!scriptName.endsWith(".groovy")) {
            scriptName += ".groovy";
        }

        File scriptFile = new File(scriptsDir, scriptName);
        if (!scriptFile.exists()) {
            sendError(exchange, 404, "Script not found: " + scriptName);
            return;
        }

        try {
            ScriptHandler handler = getOrLoadScript(scriptFile);
            handler.handle(exchange);
        } catch (Exception e) {
            LOG.severe("Error executing script " + scriptName + ": " + e.getMessage());
            sendError(exchange, 500, "Internal Script Error: " + e.getMessage());
        }
    }

    private ScriptHandler getOrLoadScript(File file) throws Exception {
        long lastModified = file.lastModified();
        ScriptCacheEntry entry = scriptCache.get(file.getAbsolutePath());

        if (entry == null || entry.lastModified < lastModified) {
            LOG.info("Loading/Reloading script: " + file.getName());
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            
            // Evaluate the script and expect it to return an implementation of ScriptHandler
            Object result = engine.eval(content);
            if (!(result instanceof ScriptHandler)) {
                throw new RuntimeException("Script " + file.getName() + " must return an instance of " + ScriptHandler.class.getName());
            }
            
            entry = new ScriptCacheEntry(lastModified, (ScriptHandler) result);
            scriptCache.put(file.getAbsolutePath(), entry);
        }
        return entry.handler;
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] response = message.getBytes();
        exchange.sendResponseHeaders(status, response.length);
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static class ScriptCacheEntry {
        final long lastModified;
        final ScriptHandler handler;

        ScriptCacheEntry(long lastModified, ScriptHandler handler) {
            this.lastModified = lastModified;
            this.handler = handler;
        }
    }
}
