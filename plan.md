# Cameo HTTP Server Plugin Plan

## Overview
Implementation of a lightweight HTTP server as a plugin within Cameo System Modeler.

## Goals
- **First Iteration**: Implement a basic HTTP server that logs incoming requests to the Cameo notification window.
- **Extensibility**: Support dynamic endpoint handling via Groovy scripts to enable fast iteration and hot-reloading without restarting the Java core.

## Technical Approach
1. **Java Core**:
    - Plugin infrastructure for Cameo System Modeler.
    - Embedded lightweight HTTP server (e.g., using `com.sun.net.httpserver.HttpServer` or a similar lightweight library).
    - Notification window integration for logging.
    - Groovy script engine integration to load and execute endpoint handlers.
2. **Dynamic Endpoints**:
    - Groovy files stored in a specific directory.
    - Mechanism to monitor the directory for changes (hot reload) or load scripts on each request.
    - Defined interface for Groovy handlers to interact with the Cameo model and API.

## Roadmap

### Iteration 1: Basic Infrastructure (Completed)
- [x] Setup basic plugin structure.
- [x] Integrate lightweight HTTP server.
- [x] Implement request logging to the notification window.

### Iteration 2: Dynamic Scripting (Completed)
- [x] Implement Groovy script loading mechanism.
- [x] Define endpoint handler API for scripts.
- [x] Delegate endpoint handling to loaded script code.
- [x] Implement hot-reload functionality for Groovy scripts.

### Iteration 3: Annotated Routing & Multi-Endpoint Scripts (Completed)
- [x] Define `@HttpEndpoint` annotation for script methods.
- [x] Implement `GroovyClassLoader` integration to compile scripts into classes.
- [x] Implement annotation scanning to map endpoints to script methods.
- [x] Create a central routing table to manage multi-script endpoint mappings.
- [x] Implement partial URL matching for flexible routing (`/hello/{name}` → path variables).
- [x] Update hot-reload to re-scan annotations and update the routing table.
- [x] Create Python integration test suite (uv + pytest + httpx) for the HTTP API.
