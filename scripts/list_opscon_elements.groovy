import com.haarer.httpserver.handlers.HttpEndpoint
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.sun.net.httpserver.HttpExchange

class OpsconElements {
    private ObjectMapper mapper = new ObjectMapper()

    /**
     * Lists all elements under a named top-level package in the model.
     *
     * GET /list_elements/{elementName}
     *
     * Scans the primary model's direct children for a Package matching
     * elementName, then recursively collects all contained elements into
     * a JSON array. Each entry includes name, type, path, and stereotypes.
     *
     * Example: /list_elements/1-OpsCon returns 199 elements from the
     * operational concept package.
     */
    @HttpEndpoint(path = "/list_elements/{elementName}", method = "GET")
    void listElements(HttpExchange exchange, Map<String, String> pathVars) {
        def results = mapper.createArrayNode()
        try {
            def elementName = pathVars["elementName"]
            def project = Application.getInstance().getProject()
            if (project != null) {
                def model = project.getPrimaryModel()
                if (model != null) {
                    for (Element child : model.getOwnedElement()) {
                        if (child instanceof Package && child.getName() == elementName) {
                            collectElements(child, results, "Model::" + elementName)
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                def err = '{"error":"' + e.getMessage().replace('"','\\"') + '"}'
                def errData = err.getBytes()
                exchange.sendResponseHeaders(500, errData.length)
                exchange.getResponseBody().write(errData)
                return
            } catch (Exception ex) {}
        }
        def json = mapper.writeValueAsString(results)
        def data = json.getBytes()
        exchange.getResponseHeaders().set("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, data.length)
        exchange.getResponseBody().write(data)
    }

    private void collectElements(Package pkg, ArrayNode target, String path) {
        for (Element child : pkg.getOwnedElement()) {
            if (child == null) continue
            def childName
            try { childName = child.getName() } catch (Exception ignored) { childName = null }
            if (childName == null) continue
            def currentPath = path + "::" + childName
            if (child instanceof Package) {
                collectElements(child, target, currentPath)
            } else {
                def entry = mapper.createObjectNode()
                entry.put("name", childName)
                entry.put("type", child.getClass().getSimpleName())
                entry.put("path", currentPath)
                def sts = child.getAppliedStereotype()
                if (sts != null) {
                    def stArr = mapper.createArrayNode()
                    for (st in sts) {
                        if (st != null) stArr.add(st.getName())
                    }
                    entry.set("stereotypes", stArr)
                }
                target.add(entry)
            }
        }
    }
}
