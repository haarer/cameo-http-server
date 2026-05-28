import com.haarer.httpserver.handlers.HttpEndpoint
import com.nomagic.magicdraw.core.Application
import com.sun.net.httpserver.HttpExchange

class LoggingDemo {
    @HttpEndpoint(path = "/logging_demo", method = "GET")
    void demo(HttpExchange exchange) {
        def guiLog = Application.getInstance().getGUILog()
        guiLog.log("=== Logging Demo Script ===")
        guiLog.log("INFO: This is a standard log message to the Cameo notification window")
        guiLog.log("WARNING: Received " + exchange.getRequestMethod() + " " + exchange.getRequestURI())

        try {
            def project = Application.getInstance().getProject()
            guiLog.log("INFO: Project loaded: " + (project != null ? project.getName() : "NONE"))
        } catch (Exception e) {
            guiLog.showError("ERROR: Failed to get project: " + e.message)
        }

        def data = "OK - check Cameo notification window for log output".getBytes("UTF-8")
        exchange.sendResponseHeaders(200, data.length)
        exchange.getResponseBody().write(data)
        exchange.getResponseBody().close()
    }
}
