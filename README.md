# Cameo HTTP Server Plugin

A lightweight HTTP server integrated as a plugin within CATIA Magic / Cameo Systems Modeler. This project provides a bridge between external HTTP requests and the Cameo JVM, allowing for automated model manipulation and introspection.

## Capabilities

### Current Version (v1.0.0)
- **Dynamic Endpoint Handling**: Request handlers can be defined in Groovy scripts, allowing for logic updates without restarting Cameo.
- **Hot Reloading**: Groovy scripts are monitored for changes and reloaded automatically on the next request.
- **Request Logging**: Built-in capability to log requests and script output directly to the Cameo notification window (GUI Log).
- **Configurable Port**: The server port can be adjusted via the system property `cameo.http.server.port` (defaults to `18741`).
- **Configurable Scripts Directory**: The location of Groovy scripts can be adjusted via the system property `cameo.http.server.scripts.dir`. Defaults to the `scripts/` subdirectory of the plugin installation.

### Planned Capabilities
- **Annotated Routing**: Ability to define multiple endpoints within a single script using annotations.
- **Advanced Routing**: Support for partial URL matching and complex routing rules.

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
    - **`DynamicScriptHandler`**: The primary handler that maps URI paths to Groovy scripts and executes them.
    - **`ScriptHandler`**: An interface that Groovy scripts must implement to handle HTTP exchanges.

### Data Flow
`HTTP Request` $\rightarrow$ `HttpServer` $\rightarrow$ `DynamicScriptHandler` $\rightarrow$ `Groovy Script` $\rightarrow$ `Cameo API / GUI Log`

## Main Functions & Interfaces

### Core Classes
- `CameoHttpServerPlugin`: The entry point for Cameo. Responsible for bootstrapping the server.
- `CameoHttpServer`: The engine that maintains the HTTP listener and maps URIs to handlers.
- `DynamicScriptHandler`: Manages the loading, caching, and hot-reloading of Groovy scripts.

### Key Interfaces
- `com.sun.net.httpserver.HttpHandler`: The primary interface used to define endpoint behavior.
- `com.haarer.httpserver.handlers.ScriptHandler`: The interface required for dynamic Groovy endpoints.
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
