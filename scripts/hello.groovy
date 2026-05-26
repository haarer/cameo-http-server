import com.haarer.httpserver.handlers.ScriptHandler;
import com.nomagic.magicdraw.core.Application;
import com.sun.net.httpserver.HttpExchange;

// This script must return an instance of ScriptHandler
new ScriptHandler() {
    @Override
    void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().toString();
        String msg = "Hello from Groovy! Received " + method + " for " + uri;
        
        Application.getInstance().getGUILog().log("Groovy Script: " + msg);
        
        byte[] response = msg.getBytes();
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().close();
    }
}
