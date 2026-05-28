import com.haarer.httpserver.handlers.HttpEndpoint
import com.sun.net.httpserver.HttpExchange

class TestEndpoint {
    @HttpEndpoint(path = "/test", method = "GET")
    void handle(HttpExchange exchange) {
        def data = "OK".getBytes()
        exchange.sendResponseHeaders(200, data.length)
        exchange.getResponseBody().write(data)
        exchange.getResponseBody().close()
    }
}
