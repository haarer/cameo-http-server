package com.haarer.httpserver;

import com.haarer.httpserver.handlers.DynamicScriptHandler;
import com.haarer.httpserver.handlers.LogRequestHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class CameoHttpServer {
    private final HttpServer server;
    private final String scriptsDir;

    public CameoHttpServer(int port) throws IOException {
        this.scriptsDir = determineDefaultScriptsDir();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        registerHandlers();
    }

    private String determineDefaultScriptsDir() {
        String propertyDir = System.getProperty("cameo.http.server.scripts.dir");
        if (propertyDir != null) {
            return propertyDir;
        }

        try {
            // Attempt to find the plugin installation directory based on the class location
            String jarPath = CameoHttpServerPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // jarPath is usually .../plugins/com.haarer.httpserver/cameo-http-server-1.0.0.jar
            int lastSlash = jarPath.lastIndexOf(File.separator);
            if (lastSlash != -1) {
                String pluginDir = jarPath.substring(0, lastSlash);
                return pluginDir + File.separator + "scripts";
            }
        } catch (Exception e) {
            // Fallback to a default workspace path for development
        }
        return "/workspace/cameo-http-server/scripts";
    }

    private void registerHandlers() {
        // Log requests at root for debugging, but let the DynamicScriptHandler handle specific paths
        // For this iteration, we'll use DynamicScriptHandler for everything to support the script logic
        server.createContext("/", new DynamicScriptHandler(scriptsDir));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(2);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }
}
