package com.haarer.httpserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * Interface for HTTP handlers implemented in Groovy.
 */
public interface ScriptHandler {
    void handle(HttpExchange exchange) throws IOException;
}
