package com.haarer.httpserver.handlers;

import com.nomagic.magicdraw.core.Application;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatedScriptHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(AnnotatedScriptHandler.class.getName());

    private final String scriptsDirPath;
    private final GroovyClassLoader gcl;
    private final Object reloadLock = new Object();

    private volatile Map<String, List<RouteEntry>> routeTable = Map.of();
    private final Map<String, Long> fileCache = new ConcurrentHashMap<>();

    private record RouteEntry(Pattern pattern, List<String> pathVarNames, Object instance, Method method) {}

    public AnnotatedScriptHandler(String scriptsDirPath) {
        this.scriptsDirPath = scriptsDirPath;
        this.gcl = new GroovyClassLoader(getClass().getClassLoader());
        reloadRoutes();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        checkForReload();

        var method = exchange.getRequestMethod().toUpperCase();
        var path = URI.create(exchange.getRequestURI().getPath()).normalize().getPath();

        var logMsg = "Cameo HTTP Server: " + method + " " + path;
        LOG.info(logMsg);
        try {
            Application.getInstance().getGUILog().log(logMsg);
        } catch (Exception ignored) {}

        // CORS preflight for all paths
        if ("OPTIONS".equals(method)) {
            var headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
            return;
        }

        var table = routeTable;
        var routes = table.get(method);
        if (routes != null) {
            for (var entry : routes) {
                var matcher = entry.pattern().matcher(path);
                if (matcher.matches()) {
                    invokeHandler(exchange, entry, matcher);
                    return;
                }
            }
        }

        sendError(exchange, 404, "No handler for " + method + " " + path);
    }

    private void invokeHandler(HttpExchange exchange, RouteEntry entry, Matcher matcher) throws IOException {
        try {
            if (entry.method().getParameterCount() <= 1) {
                entry.method().invoke(entry.instance(), exchange);
            } else {
                var pathVars = new HashMap<String, String>();
                for (int i = 0; i < entry.pathVarNames().size(); i++) {
                    pathVars.put(entry.pathVarNames().get(i), matcher.group(i + 1));
                }
                entry.method().invoke(entry.instance(), exchange, pathVars);
            }
        } catch (Exception e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            var msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
            try {
                Application.getInstance().getGUILog().showError("HTTP 500: " + msg);
            } catch (Exception ignored) {}
            sendError(exchange, 500, "Internal error: " + msg);
            return;
        }
        // Ensure response body is always closed to prevent connection leaks
        try {
            exchange.getResponseBody().close();
        } catch (Exception ignored) {}
    }

    private void checkForReload() {
        var dir = new File(scriptsDirPath);
        if (!dir.isDirectory()) return;

        for (var f : dir.listFiles((d, name) -> name.endsWith(".groovy"))) {
            var cached = fileCache.get(f.getAbsolutePath());
            if (cached == null || cached != f.lastModified()) {
                synchronized (reloadLock) {
                    reloadRoutes();
                }
                return;
            }
        }
    }

    private void reloadRoutes() {
        var dir = new File(scriptsDirPath);
        if (!dir.isDirectory()) {
            LOG.warning("Scripts directory not found: " + scriptsDirPath);
            return;
        }

        var newTable = new HashMap<String, List<RouteEntry>>();
        var newCache = new HashMap<String, Long>();

        var files = dir.listFiles((d, name) -> name.endsWith(".groovy"));
        if (files == null) return;

        for (var file : files) {
            newCache.put(file.getAbsolutePath(), file.lastModified());
            try {
                var clazz = gcl.parseClass(file);
                var instance = clazz.getDeclaredConstructor().newInstance();

                for (var method : clazz.getMethods()) {
                    var ann = method.getAnnotation(HttpEndpoint.class);
                    if (ann == null) continue;

                    var httpMethod = ann.method().toUpperCase();
                    var rawPath = ann.path();

                    var varNames = new ArrayList<String>();
                    var varMatcher = Pattern.compile("\\{([^/]+)}").matcher(rawPath);
                    while (varMatcher.find()) {
                        varNames.add(varMatcher.group(1));
                    }

                    var regex = "^" + rawPath.replaceAll("\\{[^/]+}", "([^/]+)") + "$";
                    var pattern = Pattern.compile(regex);

                    var entry = new RouteEntry(pattern, varNames, instance, method);
                    newTable.computeIfAbsent(httpMethod, k -> new ArrayList<>()).add(entry);

                    LOG.info("Mapped " + httpMethod + " " + rawPath
                            + " -> " + clazz.getSimpleName() + "." + method.getName());
                }
            } catch (Exception e) {
                LOG.warning("Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }

        // Exact matches before parameterized
        for (var list : newTable.values()) {
            list.sort((a, b) -> {
                var aParam = !a.pathVarNames().isEmpty();
                var bParam = !b.pathVarNames().isEmpty();
                if (aParam != bParam) return aParam ? 1 : -1;
                return 0;
            });
        }

        routeTable = newTable;
        fileCache.clear();
        fileCache.putAll(newCache);

        var totalRoutes = newTable.values().stream().mapToInt(List::size).sum();
        LOG.info("Loaded " + totalRoutes + " routes from " + files.length + " scripts");
        try {
            Application.getInstance().getGUILog().log(
                    "Cameo HTTP Server: Loaded " + totalRoutes + " routes from " + files.length + " scripts");
        } catch (Exception ignored) {}
    }

    private void sendError(HttpExchange exchange, int code, String msg) throws IOException {
        var data = msg.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, data.length);
        try (var os = exchange.getResponseBody()) {
            os.write(data);
        }
    }
}
