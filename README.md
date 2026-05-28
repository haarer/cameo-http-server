# Cameo HTTP Server Plugin

A lightweight HTTP server integrated as a plugin within CATIA Magic / Cameo Systems Modeler. This project provides a bridge between external HTTP requests and the Cameo JVM, allowing for automated model manipulation and introspection.

## Capabilities

### Current Version (v1.0.0)
- **Dynamic Endpoint Handling**: Request handlers can be defined in Groovy scripts, allowing for logic updates without restarting Cameo.
- **Hot Reloading**: Groovy scripts are monitored for changes and reloaded automatically on the next request.
- **Annotated Routing**: Define multiple endpoints per script using the `@HttpEndpoint` annotation on class methods.
- **Path Variables**: Routes support template variables (`/items/{id}`) extracted into a `Map<String, String>`.
- **Request Logging**: Built-in capability to log requests and script output directly to the Cameo notification window (GUI Log).
- **Configurable Port**: The server port can be adjusted via the system property `cameo.http.server.port` (defaults to `18741`).
- **Configurable Scripts Directory**: The location of Groovy scripts can be adjusted via the system property `cameo.http.server.scripts.dir`. Defaults to the `scripts/` subdirectory of the plugin installation.

## Internal Architecture

### Component Overview
The plugin follows a layered architecture to decouple the Cameo plugin lifecycle from the HTTP server logic:

1.  **Plugin Layer (`CameoHttpServerPlugin`)**:
    - Extends the Cameo `Plugin` class.
    - Manages the lifecycle of the server (`init()` and `close()`).
    - Handles initial configuration and error reporting to the GUI.

2.  **Server Layer (`CameoHttpServer`)**:
    - Wraps `com.sun.net.httpserver.HttpServer`.
    - Manages the server instance, thread pool (FixedThreadPool), and route registration.
    - Acts as the central dispatcher for incoming requests.

3.  **Handler Layer (`HttpHandler` implementations)**:
    - Implements the `com.sun.net.httpserver.HttpHandler` interface.
    - **`AnnotatedScriptHandler`**: The primary handler. Uses `GroovyClassLoader` to compile Groovy scripts into classes, scans for `@HttpEndpoint` annotations, builds a routing table with regex-based URL matching (including path variables), and hot-reloads on file changes.
    - **`HttpEndpoint`**: Annotation used in Groovy scripts to declare routes (`@HttpEndpoint(path = "/foo", method = "GET")`).

### Data Flow
`HTTP Request` $\rightarrow$ `HttpServer` $\rightarrow$ `AnnotatedScriptHandler` $\rightarrow$ `Groovy @HttpEndpoint method` $\rightarrow$ `Cameo API / GUI Log`

## Main Functions & Interfaces

### Core Classes
- `CameoHttpServerPlugin`: The entry point for Cameo. Responsible for bootstrapping the server.
- `CameoHttpServer`: The engine that maintains the HTTP listener and maps URIs to handlers.
- `AnnotatedScriptHandler`: Compiles Groovy scripts, scans `@HttpEndpoint` annotations, builds and manages the routing table with hot-reload.

### Key Interfaces
- `com.sun.net.httpserver.HttpHandler`: The primary interface used to define endpoint behavior.
- `com.haarer.httpserver.handlers.HttpEndpoint`: Annotation for declaring routes on Groovy class methods.
- `com.nomagic.magicdraw.plugins.Plugin`: The interface required for integration with the Cameo plugin architecture.

## Architecture Decisions

### 1. Use of `com.sun.net.httpserver.HttpServer`
**Decision**: Utilize the built-in JDK HTTP server instead of a heavyweight framework like Spring Boot or Jetty.
**Reasoning**: 
- **Minimal Dependencies**: Reduces the risk of classpath conflicts within the complex Cameo JVM environment.
- **Performance**: Sufficient for the intended low-latency, lightweight signaling and model manipulation tasks.
- **Simplicity**: Eases deployment and build process.

### 2. Dynamic Groovy Endpoints
**Decision**: Implement a mechanism to load endpoint logic from external `.groovy` files.
**Reasoning**: 
- **Fast Iteration**: Developing against the Cameo API typically requires frequent restarts of a large application. Groovy allows for "hot-swapping" logic.
- **Stability**: Keeps the Java core stable and minimal, while pushing volatile business logic to scripts.

### 3. Port Selection
**Decision**: Default port set to `18741`.
**Reasoning**: To avoid conflict with other common Cameo plugins, specifically the Cameo MCP Bridge which defaults to `18740`.

## Test Suite

An integration test suite is available under `tests/`. It exercises the HTTP API against a running Cameo instance.

### Deployment Topology

```
Host machine
┌──────────────────────────────────────────┐
│  Cameo + HTTP plugin                     │
│  127.0.0.1:18741                         │
│                                          │
│  socat TCP:18750 ──► localhost:18741     │
└──────────────────────┬───────────────────┘
                       │ host.containers.internal:18750
                       ▼
           ┌─────────────────────┐
           │ OpenCode container  │
           │ uv run pytest       │
           │ SERVER_URL=         │
           │ host.containers     │
           │ .internal:18750     │
           └─────────────────────┘
```

Cameo and the HTTP plugin run on the host machine, binding to `127.0.0.1:18741`. A `socat` bridge on the host forwards `TCP:18750` → `localhost:18741` to expose the port to containers. Tests (and opencode) run from inside a Podman container and reach the server via `host.containers.internal:18750`.

### Prerequisites
- [uv](https://docs.astral.sh/uv/) (Python package manager)
- A running Cameo instance with the plugin loaded

### Running

```bash
cd tests
uv run pytest -v
```

Override the server URL:

```bash
SERVER_URL=http://host.containers.internal:18750 uv run pytest -v
```

See `tests/README.md` for details.

## Example Scripts

The `scripts/` directory ships with several Groovy endpoint handlers that demonstrate the plugin's capabilities.

### `test.groovy` — Minimal endpoint

```groovy
@HttpEndpoint(path = "/test", method = "GET")
void handle(HttpExchange exchange) {
    ...
}
```

A trivial health-check endpoint. Returns `200 OK` with body `"OK"`.

### `logging_demo.groovy` — GUI Log integration

```groovy
@HttpEndpoint(path = "/logging_demo", method = "GET")
void demo(HttpExchange exchange) {
    Application.getInstance().getGUILog().log("...")
    ...
}
```

Writes messages to the Cameo notification window and returns a confirmation to the caller.

### `list_opscon_elements.groovy` — Model introspection

```
GET /list_elements/{elementName}
```

Scans the primary model for a top-level Package matching `elementName`, then recursively collects all contained elements into a JSON array. Each entry includes `name`, `type`, `path`, and `stereotypes`.

Demonstrates path variable routing (`@HttpEndpoint(path = "/list_elements/{elementName}")`) and parsing the model via the Cameo API.

### Python client example

The endpoint can be queried from any HTTP client. Below is a Python script that uses the `/list_elements` endpoint to build a package tree (showing only branch nodes):

```python
import httpx
from collections import defaultdict

r = httpx.get("http://host.containers.internal:18750/list_elements/1-OpsCon")
data = r.json()

children = defaultdict(set)
for e in data:
    parts = e["path"].split("::")
    for i in range(1, len(parts)):
        children["::".join(parts[:i])].add(parts[i])

def print_tree(node, prefix, path):
    name = path.split("::")[-1] if "::" in path else path
    kids = sorted(children.get(path, set()))
    if not kids:
        return
    print(f"{prefix}{name}/")
    for i, child in enumerate(kids):
        child_path = path + "::" + child
        is_last = i == len(kids) - 1
        ext = "└── " if is_last else "├── "
        print_tree(child, prefix + ("    " if is_last else "│   "), child_path)

for root in sorted(children):
    if "::" not in root:
        print_tree(None, "", root)
```

Output (real result from the SAF FFDS Example Model):

```
Model/
    1-OpsCon/
    │   Capabilities/
    │   │   FDN Capability/
    │   │   SAR Capability/
    │   Exchange Types/
    │   │   Fire Detection and Notification Context/
    │       Search and Rescue Context/
    │   Glossary/
    │   Operational Context/
    │   │   Performer/
        Operational Stories/
        │   FDN Process/
        │   SAR Process/
        │   Sketch/
```

## Build and Deployment
The project uses Gradle for building.

### Development Environment
To develop and build this plugin, the following tools are required:
- **OpenJDK 21**: The project is built for and targets Java 21.
- **Gradle**: Used for dependency management and plugin assembly.
- **Cameo Automaton Plugin**: Required at runtime to provide the Groovy script engine.

On Alpine Linux, these can be installed via:
`apk add openjdk21 gradle`

- **Build**: `./gradlew assemblePlugin`
- **Deploy**: `./gradlew deploy -PcameoHome="/path/to/Cameo"`

## License
This project is licensed under the Apache License 2.0.

Copyright © Alexander Haarer
